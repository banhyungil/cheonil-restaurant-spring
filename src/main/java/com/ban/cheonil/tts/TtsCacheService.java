package com.ban.cheonil.tts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

/**
 * TTS mp3 디스크 캐시.
 *
 * <p>키 = SHA-256({@code text|speed|gainDb|voice}). 파일명 {@code <hex>.mp3}. 디렉터리 총 크기 상한 도달 시 mtime 오래된
 * 순으로 evict.
 *
 * <p>키가 단방향 해시라 파일만으로는 원문 복원이 불가능해서, 관리 화면(목록)용 메타데이터를 {@code <hex>.json} sidecar 로 함께 남긴다. mp3 를
 * 지우는 모든 경로(evict / delete / clear)는 짝 json 도 함께 지워 고아 파일을 만들지 않는다.
 *
 * <p>동시 요청 dedup 은 호출 측에서 처리(현재 미적용 — 부하 보고 결정). 캐시 자체는 atomic write (tmp → rename) 라 partial read 안전.
 */
@Service
public class TtsCacheService {

  private static final Logger log = LoggerFactory.getLogger(TtsCacheService.class);

  /** SHA-256 hex 64자. 외부 입력(삭제 API path variable) 의 path traversal 방어용. */
  private static final Pattern KEY_PATTERN = Pattern.compile("[0-9a-f]{64}");

  private final Path cacheDir;
  private final long maxSizeBytes;
  private final ObjectMapper objectMapper;

  public TtsCacheService(
      @Value("${tts.cache.dir:#{systemProperties['java.io.tmpdir']}/melotts-cache}") String cacheDir,
      @Value("${tts.cache.max-size-mb:200}") long maxSizeMb,
      ObjectMapper objectMapper) {
    this.cacheDir = Path.of(cacheDir);
    this.maxSizeBytes = maxSizeMb * 1024L * 1024L;
    this.objectMapper = objectMapper;
  }

  /** sidecar 에 저장하는 합성 파라미터 — 목록 화면에서 문구/화자를 되살리기 위함. */
  public record CacheMeta(
      String text, double speed, int gainDb, String voice, OffsetDateTime createdAt) {}

  /**
   * 관리 화면 목록 1건 — sidecar 메타 + 파일 실측값.
   *
   * @param lastUsedAt mp3 mtime. {@link #get} 이 hit 마다 갱신하므로 마지막 사용 시각의 근사값.
   */
  public record CacheEntry(
      String key,
      String text,
      double speed,
      int gainDb,
      String voice,
      long sizeBytes,
      OffsetDateTime createdAt,
      OffsetDateTime lastUsedAt) {}

  @PostConstruct
  void init() throws IOException {
    Files.createDirectories(cacheDir);
    log.info("tts cache dir={} maxSize={}MB", cacheDir, maxSizeBytes / 1024 / 1024);
  }

  public String keyOf(String text, double speed, int gainDb, String voice) {
    String raw = text + "|" + speed + "|" + gainDb + "|" + voice;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public Path pathOf(String key) {
    return cacheDir.resolve(key + ".mp3");
  }

  private Path metaPathOf(String key) {
    return cacheDir.resolve(key + ".json");
  }

  /** 캐시 hit → bytes, miss → null. */
  public byte[] get(String key) {
    Path p = pathOf(key);
    if (!Files.isRegularFile(p)) return null;
    try {
      // atime 갱신 — eviction LRU 근사용. 실패해도 무시.
      try {
        Files.setLastModifiedTime(
            p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
      } catch (IOException ignored) {
      }
      return Files.readAllBytes(p);
    } catch (IOException e) {
      log.warn("cache read failed key={}", key, e);
      return null;
    }
  }

  /** atomic write (tmp → rename) 후 size-limit eviction. meta 는 목록 화면용 sidecar. */
  public void put(String key, byte[] bytes, CacheMeta meta) {
    try {
      writeAtomic(pathOf(key), bytes);
    } catch (IOException e) {
      log.warn("cache write failed key={}", key, e);
      return;
    }
    // sidecar 실패는 캐시 자체를 무효화하지 않음 — 목록에만 안 뜬다.
    // Jackson 3 의 직렬화 예외는 unchecked 라 RuntimeException 도 함께 잡는다.
    try {
      writeAtomic(metaPathOf(key), objectMapper.writeValueAsBytes(meta));
    } catch (IOException | RuntimeException e) {
      log.warn("cache meta write failed key={}", key, e);
    }
    evictIfNeeded();
  }

  /**
   * sidecar 가 없는 캐시 파일에 메타데이터를 뒤늦게 채운다 — 캐시 hit 경로에서 호출.
   *
   * <p>메타데이터 도입 이전에 쌓인 mp3 는 파일명(해시)만으로는 문구를 복원할 수 없어 관리 목록에서 빠진다. 하지만 hit 시점엔 호출부가 원본
   * 파라미터를 들고 있으므로, 그 값으로 sidecar 를 만들어 주면 재사용되는 항목부터 목록에 편입된다.
   *
   * <p>{@code createdAt} 은 파일 생성 시각(birth time) 으로 근사한다 — mtime 은 {@link #get} 이 hit 마다 갱신해서
   * 쓸 수 없다. 이미 sidecar 가 있으면 no-op.
   */
  public void backfillMetaIfMissing(
      String key, String text, double speed, int gainDb, String voice) {
    Path metaPath = metaPathOf(key);
    if (Files.isRegularFile(metaPath)) return;

    Path mp3 = pathOf(key);
    if (!Files.isRegularFile(mp3)) return;

    try {
      BasicFileAttributes attrs = Files.readAttributes(mp3, BasicFileAttributes.class);
      OffsetDateTime createdAt =
          OffsetDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
      writeAtomic(
          metaPath,
          objectMapper.writeValueAsBytes(new CacheMeta(text, speed, gainDb, voice, createdAt)));
      log.debug("cache meta backfilled key={}", key);
    } catch (IOException | RuntimeException e) {
      // 실패해도 재생에는 영향 없음 — 목록에 안 뜰 뿐.
      log.warn("cache meta backfill failed key={}", key, e);
    }
  }

  private void writeAtomic(Path target, byte[] bytes) throws IOException {
    Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
    try {
      Files.write(tmp, bytes);
      Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException ignored) {
      }
      throw e;
    }
  }

  /**
   * 관리 화면 목록 — 최근 사용순.
   *
   * <p>sidecar 가 없는 파일(메타데이터 도입 이전에 쌓인 캐시) 은 문구를 알 수 없어 제외한다. {@link #clear} 로 정리 가능.
   */
  public List<CacheEntry> list() {
    try (var stream = Files.list(cacheDir)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(".mp3"))
          .map(this::toEntry)
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(CacheEntry::lastUsedAt).reversed())
          .toList();
    } catch (IOException e) {
      log.warn("cache list failed", e);
      return List.of();
    }
  }

  /** mp3 + sidecar → CacheEntry. sidecar 없거나 깨졌으면 null. */
  private CacheEntry toEntry(Path mp3) {
    String fileName = mp3.getFileName().toString();
    String key = fileName.substring(0, fileName.length() - ".mp3".length());
    Path metaPath = metaPathOf(key);
    if (!Files.isRegularFile(metaPath)) return null;
    try {
      CacheMeta meta = objectMapper.readValue(Files.readAllBytes(metaPath), CacheMeta.class);
      return new CacheEntry(
          key,
          meta.text(),
          meta.speed(),
          meta.gainDb(),
          meta.voice(),
          Files.size(mp3),
          meta.createdAt(),
          OffsetDateTime.ofInstant(
              Files.getLastModifiedTime(mp3).toInstant(), ZoneId.systemDefault()));
    } catch (IOException | RuntimeException e) {
      // 깨진 sidecar 하나가 목록 전체를 막지 않도록 해당 항목만 skip.
      log.warn("cache meta read failed key={}", key, e);
      return null;
    }
  }

  /**
   * 단건 삭제 (mp3 + sidecar).
   *
   * @return mp3 가 실제로 존재해 지워졌으면 true
   */
  public boolean delete(String key) {
    if (!KEY_PATTERN.matcher(key).matches()) {
      // 외부 입력이 그대로 경로에 붙는 자리 — 형식 위반은 조용히 거절.
      return false;
    }
    return deleteFiles(key);
  }

  /**
   * 다중 삭제 — 관리 화면에서 체크한 항목들.
   *
   * <p>일부 key 가 이미 없거나 형식이 어긋나도 나머지는 계속 지운다. 반환값은 실제로 지워진 개수.
   */
  public int deleteAll(Collection<String> keys) {
    int deleted = 0;
    for (String key : keys) {
      if (delete(key)) deleted++;
    }
    log.info("tts cache deleted — {}/{} entries", deleted, keys.size());
    return deleted;
  }

  private boolean deleteFiles(String key) {
    boolean existed = false;
    try {
      existed = Files.deleteIfExists(pathOf(key));
    } catch (IOException e) {
      log.warn("cache delete failed key={}", key, e);
    }
    try {
      Files.deleteIfExists(metaPathOf(key));
    } catch (IOException e) {
      log.warn("cache meta delete failed key={}", key, e);
    }
    return existed;
  }

  private void evictIfNeeded() {
    try (var stream = Files.list(cacheDir)) {
      var files =
          stream
              .filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().endsWith(".mp3"))
              .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
              .toList();

      // 용량 산정은 mp3 기준 — sidecar 는 수백 바이트라 무시.
      long total = 0;
      for (Path p : files) total += Files.size(p);
      if (total <= maxSizeBytes) return;

      for (Path p : files) {
        if (total <= maxSizeBytes) break;
        long size = Files.size(p);
        String fileName = p.getFileName().toString();
        String key = fileName.substring(0, fileName.length() - ".mp3".length());
        if (deleteFiles(key)) {
          total -= size;
          log.debug("evicted {} ({} bytes)", fileName, size);
        }
      }
    } catch (IOException e) {
      log.warn("evict scan failed", e);
    }
  }
}
