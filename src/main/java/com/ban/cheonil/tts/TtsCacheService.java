package com.ban.cheonil.tts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * TTS mp3 디스크 캐시.
 *
 * <p>키 = SHA-256({@code text + "|" + speed}). 파일명 {@code <hex>.mp3}. 디렉터리 총 크기 상한 도달 시 mtime 오래된 순으로 evict.
 *
 * <p>동시 요청 dedup 은 호출 측에서 처리(현재 미적용 — 부하 보고 결정). 캐시 자체는 atomic write (tmp → rename) 라 partial read 안전.
 */
@Service
public class TtsCacheService {

  private static final Logger log = LoggerFactory.getLogger(TtsCacheService.class);

  private final Path cacheDir;
  private final long maxSizeBytes;

  public TtsCacheService(
      @Value("${tts.cache.dir:#{systemProperties['java.io.tmpdir']}/melotts-cache}") String cacheDir,
      @Value("${tts.cache.max-size-mb:200}") long maxSizeMb) {
    this.cacheDir = Path.of(cacheDir);
    this.maxSizeBytes = maxSizeMb * 1024L * 1024L;
  }

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

  /** 캐시 hit → bytes, miss → null. */
  public byte[] get(String key) {
    Path p = pathOf(key);
    if (!Files.isRegularFile(p)) return null;
    try {
      // atime 갱신 — eviction LRU 근사용. 실패해도 무시.
      try {
        Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
      } catch (IOException ignored) {
      }
      return Files.readAllBytes(p);
    } catch (IOException e) {
      log.warn("cache read failed key={}", key, e);
      return null;
    }
  }

  /** atomic write (tmp → rename) 후 size-limit eviction. */
  public void put(String key, byte[] bytes) {
    Path target = pathOf(key);
    Path tmp = cacheDir.resolve(key + ".mp3.tmp");
    try {
      Files.write(tmp, bytes);
      Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      log.warn("cache write failed key={}", key, e);
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException ignored) {
      }
      return;
    }
    evictIfNeeded();
  }

  private void evictIfNeeded() {
    try (var stream = Files.list(cacheDir)) {
      var files =
          stream
              .filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().endsWith(".mp3"))
              .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
              .toList();

      long total = 0;
      for (Path p : files) total += Files.size(p);
      if (total <= maxSizeBytes) return;

      for (Path p : files) {
        if (total <= maxSizeBytes) break;
        long size = Files.size(p);
        try {
          Files.delete(p);
          total -= size;
          log.debug("evicted {} ({} bytes)", p.getFileName(), size);
        } catch (IOException e) {
          log.warn("evict failed {}", p, e);
        }
      }
    } catch (IOException e) {
      log.warn("evict scan failed", e);
    }
  }
}
