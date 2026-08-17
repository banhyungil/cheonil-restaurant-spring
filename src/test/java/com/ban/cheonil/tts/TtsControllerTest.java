package com.ban.cheonil.tts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ban.cheonil.tts.TtsCacheService.CacheEntry;
import com.ban.cheonil.tts.TtsService.Result;

/**
 * {@link TtsController} 웹 계층 테스트.
 *
 * <p>Google 호출/파일 I/O 는 mock — 요청 검증(400 조건), 캐시 상태 헤더, 캐시 관리 API 의 상태코드 매핑만 본다.
 *
 * <p>경로에 {@code /api} 가 붙는 것은 {@code WebConfig.addPathPrefix} 규칙 때문 — 슬라이스 테스트에도 그대로 적용된다.
 */
@WebMvcTest(TtsController.class)
class TtsControllerTest {

  private static final String KEY = "a".repeat(64);

  @Autowired MockMvc mvc;

  @MockitoBean TtsService ttsService;
  @MockitoBean TtsCacheService cache;

  @Nested
  @DisplayName("GET /api/tts")
  class Synthesize {

    @Test
    @DisplayName("합성 성공 시 mp3 와 X-Cache 헤더 반환")
    void 합성_성공() throws Exception {
      given(ttsService.synthesize(anyString(), anyDouble(), anyInt(), any()))
          .willReturn(new Result(new byte[] {1, 2, 3}, false));

      mvc.perform(get("/api/tts").param("text", "1번매장"))
          .andExpect(status().isOk())
          .andExpect(content().contentType("audio/mpeg"))
          .andExpect(header().string("X-Cache", "MISS"));
    }

    @Test
    @DisplayName("캐시 hit 이면 X-Cache=HIT")
    void 캐시_hit() throws Exception {
      given(ttsService.synthesize(anyString(), anyDouble(), anyInt(), any()))
          .willReturn(new Result(new byte[] {1}, true));

      mvc.perform(get("/api/tts").param("text", "1번매장"))
          .andExpect(status().isOk())
          .andExpect(header().string("X-Cache", "HIT"));
    }

    @Test
    @DisplayName("모르는 파라미터(_r) 는 무시 — 클라이언트의 브라우저 캐시 버스터가 합성 결과에 영향 주면 안 된다")
    void 캐시버스터_파라미터는_무시() throws Exception {
      given(ttsService.synthesize(anyString(), anyDouble(), anyInt(), any()))
          .willReturn(new Result(new byte[] {1}, false));

      mvc.perform(get("/api/tts").param("text", "1번매장").param("_r", "1761000000-1"))
          .andExpect(status().isOk());

      // _r 이 캐시 키 계산에 섞이지 않도록, 서비스에는 원래 4개 인자만 그대로 전달된다.
      verify(ttsService).synthesize("1번매장", 1.0, 0, null);
    }

    @Test
    @DisplayName("빈 문구 / 500자 초과 / gainDb 범위 밖은 400 — Google 호출 자체를 안 한다")
    void 잘못된_요청은_400() throws Exception {
      mvc.perform(get("/api/tts").param("text", " ")).andExpect(status().isBadRequest());
      mvc.perform(get("/api/tts").param("text", "가".repeat(501)))
          .andExpect(status().isBadRequest());
      mvc.perform(get("/api/tts").param("text", "정상").param("gainDb", "13"))
          .andExpect(status().isBadRequest());
      mvc.perform(get("/api/tts").param("text", "정상").param("gainDb", "-1"))
          .andExpect(status().isBadRequest());

      verify(ttsService, never()).synthesize(anyString(), anyDouble(), anyInt(), any());
    }
  }

  @Nested
  @DisplayName("캐시 관리 API")
  class CacheApi {

    @Test
    @DisplayName("목록 — sidecar 메타가 그대로 JSON 으로 나온다")
    void 목록() throws Exception {
      given(cache.list())
          .willReturn(
              List.of(
                  new CacheEntry(
                      KEY,
                      "김치찌개2개 해주세요",
                      1.2,
                      3,
                      "ko-KR-Chirp3-HD-Aoede",
                      6432,
                      OffsetDateTime.now(),
                      OffsetDateTime.now())));

      mvc.perform(get("/api/tts/cache"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].key").value(KEY))
          .andExpect(jsonPath("$[0].text").value("김치찌개2개 해주세요"))
          .andExpect(jsonPath("$[0].voice").value("ko-KR-Chirp3-HD-Aoede"))
          .andExpect(jsonPath("$[0].sizeBytes").value(6432));
    }

    @Test
    @DisplayName("단건 삭제 성공 → 204")
    void 삭제_204() throws Exception {
      given(cache.delete(KEY)).willReturn(true);

      mvc.perform(delete("/api/tts/cache/" + KEY)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("대상이 없거나 키 형식이 틀리면 → 404")
    void 삭제_404() throws Exception {
      given(cache.delete(anyString())).willReturn(false);

      mvc.perform(delete("/api/tts/cache/" + KEY)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다중 삭제 → 204, 받은 key 목록 그대로 위임")
    void 다중삭제_204() throws Exception {
      String other = "b".repeat(64);

      mvc.perform(
              delete("/api/tts/cache")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"keys\":[\"" + KEY + "\",\"" + other + "\"]}"))
          .andExpect(status().isNoContent());

      verify(cache).deleteAll(List.of(KEY, other));
    }

    @Test
    @DisplayName("빈 key 목록은 400 — 실수로 전체가 지워지는 경로를 남기지 않는다")
    void 빈_목록은_400() throws Exception {
      mvc.perform(
              delete("/api/tts/cache")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"keys\":[]}"))
          .andExpect(status().isBadRequest());

      verify(cache, never()).deleteAll(any());
    }
  }
}
