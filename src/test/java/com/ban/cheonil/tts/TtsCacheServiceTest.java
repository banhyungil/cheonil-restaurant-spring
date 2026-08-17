package com.ban.cheonil.tts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ban.cheonil.tts.TtsCacheService.CacheEntry;
import com.ban.cheonil.tts.TtsCacheService.CacheMeta;

import tools.jackson.databind.ObjectMapper;

/**
 * {@link TtsCacheService} 단위 테스트 — 실제 파일시스템({@code @TempDir}) 사용.
 *
 * <p>Google 호출은 이 클래스 밖(TtsService)이라 mock 없이 캐시 I/O 만 검증한다. 핵심 불변식 두 가지:
 *
 * <ul>
 *   <li>mp3 와 sidecar json 은 항상 쌍으로 생기고 쌍으로 사라진다 (고아 파일 금지)
 *   <li>외부 입력 key 는 파일 경로에 붙기 전에 형식 검증을 통과해야 한다 (path traversal 방어)
 * </ul>
 */
class TtsCacheServiceTest {

  @TempDir Path tempDir;

  TtsCacheService cache;

  /** 200MB — eviction 이 끼어들지 않는 넉넉한 기본값. eviction 검증만 별도 인스턴스로 좁게 만든다. */
  @BeforeEach
  void setUp() throws IOException {
    cache = newCache(200);
  }

  private TtsCacheService newCache(long maxSizeMb) throws IOException {
    TtsCacheService c = new TtsCacheService(tempDir.toString(), maxSizeMb, new ObjectMapper());
    // @PostConstruct 는 Spring 컨텍스트 없이는 안 불리므로 직접 호출.
    c.init();
    return c;
  }

  private static CacheMeta meta(String text) {
    return new CacheMeta(text, 1.2, 0, "ko-KR-Chirp3-HD-Achernar", OffsetDateTime.now());
  }

  private Path mp3Of(String key) {
    return tempDir.resolve(key + ".mp3");
  }

  private Path jsonOf(String key) {
    return tempDir.resolve(key + ".json");
  }

  @Nested
  @DisplayName("keyOf")
  class KeyOf {

    @Test
    @DisplayName("같은 입력이면 항상 같은 키 — 캐시 hit 의 전제")
    void 같은_입력은_같은_키() {
      String a = cache.keyOf("1번매장", 1.2, 0, "voice-A");
      String b = cache.keyOf("1번매장", 1.2, 0, "voice-A");

      assertThat(a).isEqualTo(b).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("파라미터 하나만 달라도 다른 키 — 화자/속도/음량 변경분이 섞이지 않는다")
    void 파라미터가_다르면_다른_키() {
      String base = cache.keyOf("1번매장", 1.2, 0, "voice-A");

      assertThat(cache.keyOf("2번매장", 1.2, 0, "voice-A")).isNotEqualTo(base);
      assertThat(cache.keyOf("1번매장", 1.5, 0, "voice-A")).isNotEqualTo(base);
      assertThat(cache.keyOf("1번매장", 1.2, 3, "voice-A")).isNotEqualTo(base);
      assertThat(cache.keyOf("1번매장", 1.2, 0, "voice-B")).isNotEqualTo(base);
    }
  }

  @Nested
  @DisplayName("put / get")
  class PutGet {

    @Test
    @DisplayName("put 하면 mp3 와 sidecar json 이 쌍으로 생기고 get 으로 원본이 돌아온다")
    void 왕복() {
      String key = cache.keyOf("1번매장", 1.2, 0, "voice-A");
      byte[] bytes = {1, 2, 3, 4};

      cache.put(key, bytes, meta("1번매장"));

      assertThat(cache.get(key)).isEqualTo(bytes);
      assertThat(mp3Of(key)).isRegularFile();
      assertThat(jsonOf(key)).isRegularFile();
    }

    @Test
    @DisplayName("없는 키는 null — 호출부가 miss 로 판정")
    void 미스는_null() {
      assertThat(cache.get(cache.keyOf("없는문구", 1.0, 0, "voice-A"))).isNull();
    }

    @Test
    @DisplayName("tmp 파일을 남기지 않는다 — atomic write 후 정리 확인")
    void tmp_잔여물_없음() throws IOException {
      String key = cache.keyOf("1번매장", 1.2, 0, "voice-A");

      cache.put(key, new byte[] {1}, meta("1번매장"));

      try (var stream = Files.list(tempDir)) {
        assertThat(stream.map(p -> p.getFileName().toString()))
            .noneMatch(name -> name.endsWith(".tmp"));
      }
    }
  }

  @Nested
  @DisplayName("list")
  class ListEntries {

    @Test
    @DisplayName("sidecar 메타를 그대로 되살려 반환 — 해시 파일명으로는 복원 불가한 문구/화자")
    void 메타_복원() {
      String key = cache.keyOf("김치찌개2개 해주세요", 1.5, 3, "voice-B");
      cache.put(
          key,
          new byte[] {1, 2, 3},
          new CacheMeta("김치찌개2개 해주세요", 1.5, 3, "voice-B", OffsetDateTime.now()));

      List<CacheEntry> entries = cache.list();

      assertThat(entries).hasSize(1);
      assertThat(entries.getFirst())
          .satisfies(
              e -> {
                assertThat(e.key()).isEqualTo(key);
                assertThat(e.text()).isEqualTo("김치찌개2개 해주세요");
                assertThat(e.speed()).isEqualTo(1.5);
                assertThat(e.gainDb()).isEqualTo(3);
                assertThat(e.voice()).isEqualTo("voice-B");
                assertThat(e.sizeBytes()).isEqualTo(3);
              });
    }

    @Test
    @DisplayName("최근 사용순 정렬 — 관리 화면 상단에 방금 쓴 음성이 온다")
    void 최근_사용순() throws IOException {
      String older = putWithMtime("오래된 문구", 1_000_000L);
      String newer = putWithMtime("최근 문구", 9_000_000L);

      assertThat(cache.list()).extracting(CacheEntry::key).containsExactly(newer, older);
    }

    @Test
    @DisplayName("sidecar 없는 mp3 는 제외 — 메타데이터 도입 이전에 쌓인 캐시")
    void 메타_없는_파일_제외() throws IOException {
      Files.write(tempDir.resolve("a".repeat(64) + ".mp3"), new byte[] {1});
      String withMeta = cache.keyOf("정상 문구", 1.2, 0, "voice-A");
      cache.put(withMeta, new byte[] {1}, meta("정상 문구"));

      assertThat(cache.list()).extracting(CacheEntry::key).containsExactly(withMeta);
    }

    @Test
    @DisplayName("깨진 sidecar 는 그 항목만 skip — 목록 전체가 죽지 않는다")
    void 깨진_메타는_해당_항목만_skip() throws IOException {
      String broken = cache.keyOf("깨진 문구", 1.2, 0, "voice-A");
      cache.put(broken, new byte[] {1}, meta("깨진 문구"));
      Files.writeString(jsonOf(broken), "{ not json");

      String healthy = cache.keyOf("정상 문구", 1.2, 0, "voice-A");
      cache.put(healthy, new byte[] {1}, meta("정상 문구"));

      assertThat(cache.list()).extracting(CacheEntry::key).containsExactly(healthy);
    }

    /** 지정한 mtime 으로 캐시 1건 생성 — lastUsedAt 정렬 검증용. */
    private String putWithMtime(String text, long millis) throws IOException {
      String key = cache.keyOf(text, 1.2, 0, "voice-A");
      cache.put(key, new byte[] {1}, meta(text));
      Files.setLastModifiedTime(mp3Of(key), FileTime.fromMillis(millis));
      return key;
    }
  }

  @Nested
  @DisplayName("backfillMetaIfMissing")
  class Backfill {

    @Test
    @DisplayName("sidecar 없는 구 캐시가 재사용되면 메타가 채워져 목록에 편입된다")
    void 구_캐시_편입() throws IOException {
      // 메타데이터 도입 이전 상태 재현 — mp3 만 있고 json 없음.
      String key = cache.keyOf("옛날 문구", 1.2, 0, "voice-A");
      Files.write(mp3Of(key), new byte[] {1, 2});
      assertThat(cache.list()).isEmpty();

      cache.backfillMetaIfMissing(key, "옛날 문구", 1.2, 0, "voice-A");

      assertThat(jsonOf(key)).isRegularFile();
      assertThat(cache.list())
          .singleElement()
          .satisfies(
              e -> {
                assertThat(e.key()).isEqualTo(key);
                assertThat(e.text()).isEqualTo("옛날 문구");
                assertThat(e.voice()).isEqualTo("voice-A");
              });
    }

    @Test
    @DisplayName("이미 sidecar 가 있으면 덮어쓰지 않는다")
    void 기존_메타_보존() {
      String key = cache.keyOf("문구", 1.2, 0, "voice-A");
      cache.put(key, new byte[] {1}, meta("원래 문구"));

      cache.backfillMetaIfMissing(key, "덮어쓸 문구", 1.2, 0, "voice-A");

      assertThat(cache.list()).singleElement().satisfies(e -> assertThat(e.text()).isEqualTo("원래 문구"));
    }

    @Test
    @DisplayName("mp3 자체가 없으면 아무것도 만들지 않는다 — 유령 목록 항목 방지")
    void mp3_없으면_무시() {
      String key = cache.keyOf("없는 문구", 1.2, 0, "voice-A");

      cache.backfillMetaIfMissing(key, "없는 문구", 1.2, 0, "voice-A");

      assertThat(jsonOf(key)).doesNotExist();
      assertThat(cache.list()).isEmpty();
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("mp3 와 sidecar 를 함께 삭제 — 고아 json 을 남기지 않는다")
    void 쌍으로_삭제() {
      String key = cache.keyOf("1번매장", 1.2, 0, "voice-A");
      cache.put(key, new byte[] {1}, meta("1번매장"));

      assertThat(cache.delete(key)).isTrue();
      assertThat(mp3Of(key)).doesNotExist();
      assertThat(jsonOf(key)).doesNotExist();
    }

    @Test
    @DisplayName("없는 키는 false — 컨트롤러가 404 로 응답")
    void 없는_키는_false() {
      assertThat(cache.delete("b".repeat(64))).isFalse();
    }

    @Test
    @DisplayName("키 형식이 어긋나면 파일에 손대지 않는다 — path traversal 방어")
    void 잘못된_키는_거절() throws IOException {
      Path outside = tempDir.resolve("secret.mp3");
      Files.write(outside, new byte[] {1});

      assertThat(cache.delete("../secret")).isFalse();
      assertThat(cache.delete("secret")).isFalse();
      assertThat(cache.delete("A".repeat(64))).isFalse(); // 대문자 hex 도 거절
      assertThat(outside).exists();
    }
  }

  @Nested
  @DisplayName("deleteAll")
  class DeleteAll {

    @Test
    @DisplayName("체크한 항목만 지우고 나머지는 남긴다")
    void 선택_삭제() {
      String a = cache.keyOf("문구1", 1.2, 0, "voice-A");
      String b = cache.keyOf("문구2", 1.2, 0, "voice-A");
      String keep = cache.keyOf("문구3", 1.2, 0, "voice-A");
      cache.put(a, new byte[] {1}, meta("문구1"));
      cache.put(b, new byte[] {1}, meta("문구2"));
      cache.put(keep, new byte[] {1}, meta("문구3"));

      assertThat(cache.deleteAll(List.of(a, b))).isEqualTo(2);

      assertThat(mp3Of(a)).doesNotExist();
      assertThat(jsonOf(a)).doesNotExist();
      assertThat(mp3Of(b)).doesNotExist();
      assertThat(cache.list()).extracting(CacheEntry::key).containsExactly(keep);
    }

    @Test
    @DisplayName("없는 키·잘못된 키가 섞여도 나머지는 지우고, 실제로 지운 개수만 반환")
    void 일부_실패해도_계속() {
      String ok = cache.keyOf("문구1", 1.2, 0, "voice-A");
      cache.put(ok, new byte[] {1}, meta("문구1"));

      int deleted = cache.deleteAll(List.of("../secret", "d".repeat(64), ok));

      assertThat(deleted).isEqualTo(1);
      assertThat(mp3Of(ok)).doesNotExist();
    }
  }

  @Nested
  @DisplayName("eviction")
  class Eviction {

    /** 상한 1MB 에 600KB 씩 3건 — 오래된 순으로 밀려나야 한다. */
    @Test
    @DisplayName("상한 초과 시 오래된 것부터 mp3+json 쌍으로 evict")
    void 상한_초과시_오래된_순() throws IOException {
      TtsCacheService small = newCache(1);
      byte[] payload = new byte[600 * 1024];

      String oldest = put(small, payload, "문구1", 1_000_000L);
      String middle = put(small, payload, "문구2", 5_000_000L);
      String newest = put(small, payload, "문구3", 9_000_000L);

      // 3번째 put 직후 총 1.8MB > 1MB → 1.0MB 이하가 될 때까지 오래된 순으로 제거.
      assertThat(mp3Of(oldest)).doesNotExist();
      assertThat(jsonOf(oldest)).doesNotExist();
      assertThat(mp3Of(middle)).doesNotExist();
      assertThat(jsonOf(middle)).doesNotExist();
      assertThat(mp3Of(newest)).isRegularFile();
      assertThat(jsonOf(newest)).isRegularFile();
    }

    /**
     * put 후 mtime 을 고정 — evict 순서(mtime 오름차순)를 결정적으로 만든다.
     *
     * <p>put 안에서 evict 가 도는 구조라, 파일이 이미 밀려났으면 mtime 설정은 skip.
     */
    private String put(TtsCacheService target, byte[] payload, String text, long millis)
        throws IOException {
      String key = target.keyOf(text, 1.2, 0, "voice-A");
      target.put(key, payload, meta(text));
      if (Files.exists(mp3Of(key))) {
        Files.setLastModifiedTime(mp3Of(key), FileTime.fromMillis(millis));
      }
      return key;
    }
  }
}
