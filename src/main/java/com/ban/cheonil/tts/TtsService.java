package com.ban.cheonil.tts;

import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * Google Cloud Text-to-Speech 프록시 + 디스크 캐시.
 *
 * <p>흐름: key 계산 → 캐시 hit ? 즉시 반환 : Google API 호출 → 캐시 저장 → 반환.
 *
 * <p>API key 는 헤더({@code X-Goog-Api-Key}) 로 전달 — query string 보다 로그/프록시 노출 ↓.
 * 응답은 {@code {audioContent: <base64 mp3>}} 라 base64 디코딩 필요.
 *
 * <p>{@link com.ban.cheonil.speech.SpeechService} 의 RestTemplate 패턴을 따른다.
 */
@Service
@RequiredArgsConstructor
public class TtsService {

  private static final String GOOGLE_TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";

  private static final Logger log = LoggerFactory.getLogger(TtsService.class);

  private final RestTemplate restTemplate = new RestTemplate();
  private final TtsCacheService cache;

  @Value("${google.tts.api-key}")
  private String apiKey;

  /** 한국어 화자 기본값 — 요청에 voice 미지정 시 사용. env 로 override 가능. */
  @Value("${google.tts.voice:ko-KR-Chirp3-HD-Achernar}")
  private String defaultVoice;

  @Value("${google.tts.language-code:ko-KR}")
  private String languageCode;

  public record Result(byte[] bytes, boolean cacheHit) {}

  /**
   * @param voice null/blank 시 {@link #defaultVoice} 사용. 그 외 풀 네임 그대로 (예: {@code
   *     ko-KR-Chirp3-HD-Aoede}).
   */
  public Result synthesize(String text, double speed, int gainDb, String voice) {
    String effectiveVoice = (voice == null || voice.isBlank()) ? defaultVoice : voice;
    String key = cache.keyOf(text, speed, gainDb, effectiveVoice);

    byte[] cached = cache.get(key);
    if (cached != null) {
      log.debug("tts cache hit key={} len={}", key.substring(0, 8), text.length());
      return new Result(cached, true);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Goog-Api-Key", apiKey);

    // Google volumeGainDb 허용 범위: -96 ~ +16. 우리는 0~12 로 노출.
    Map<String, Object> body =
        Map.of(
            "input", Map.of("text", text),
            "voice", Map.of("languageCode", languageCode, "name", effectiveVoice),
            "audioConfig",
                Map.of(
                    "audioEncoding", "MP3",
                    "speakingRate", speed,
                    "volumeGainDb", gainDb));

    @SuppressWarnings("unchecked")
    Map<String, Object> res =
        restTemplate.postForObject(GOOGLE_TTS_URL, new HttpEntity<>(body, headers), Map.class);

    if (res == null || !res.containsKey("audioContent")) {
      throw new IllegalStateException("Google TTS returned no audioContent");
    }
    byte[] bytes = Base64.getDecoder().decode((String) res.get("audioContent"));

    cache.put(key, bytes);
    log.debug(
        "tts cache miss key={} len={} bytes={}",
        key.substring(0, 8),
        text.length(),
        bytes.length);
    return new Result(bytes, false);
  }
}
