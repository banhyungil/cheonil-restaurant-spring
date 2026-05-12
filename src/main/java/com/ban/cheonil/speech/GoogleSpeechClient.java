package com.ban.cheonil.speech;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Google Cloud Speech-to-Text REST 호출.
 *
 * <p>Whisper STT 가 정확도 떨어질 때 fallback 으로 사용. quota 초과는 Google 콘솔에서 cap 설정 — 초과 시 HTTP 429
 * 자동 반환되며 {@link org.springframework.web.client.HttpStatusCodeException} 로 propagate.
 *
 * <p>API key 는 헤더({@code X-Goog-Api-Key}) 로 전달 — 로그/프록시 노출 ↓.
 */
@Component
public class GoogleSpeechClient {

  private static final String GOOGLE_STT_URL = "https://speech.googleapis.com/v1/speech:recognize";

  private static final Logger log = LoggerFactory.getLogger(GoogleSpeechClient.class);

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${google.api-key}")
  private String apiKey;

  @Value("${google.stt.language-code:ko-KR}")
  private String languageCode;

  @Value("${google.stt.model:short}")
  private String model;

  /**
   * @param contentType 클라이언트 업로드 시 MIME (예: audio/webm). 비어있으면 webm 가정.
   * @param phrases speechContexts 로 전달할 단어/구문 — 매장명/메뉴명 등. 빈 리스트면 미적용.
   */
  public String transcribe(byte[] audio, String contentType, List<String> phrases) {
    String encoding = mapEncoding(contentType);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Goog-Api-Key", apiKey);

    // config 객체 — phrases 있을 때만 speechContexts 추가 (mutable map 으로 build).
    java.util.Map<String, Object> config = new java.util.HashMap<>();
    config.put("encoding", encoding);
    config.put("languageCode", languageCode);
    config.put("model", model);
    config.put("enableAutomaticPunctuation", false);
    if (phrases != null && !phrases.isEmpty()) {
      // boost 범위 0~20. 15 정도면 강한 bias — 매장명/메뉴는 closed-set 이라 강하게 줘도 OK.
      config.put(
          "speechContexts",
          List.of(Map.of("phrases", new ArrayList<>(phrases), "boost", 15.0)));
    }

    Map<String, Object> body =
        Map.of(
            "config", config,
            "audio", Map.of("content", Base64.getEncoder().encodeToString(audio)));

    @SuppressWarnings("unchecked")
    Map<String, Object> res =
        restTemplate.postForObject(
            GOOGLE_STT_URL, new HttpEntity<>(body, headers), Map.class);

    return extractTranscript(res);
  }

  /** MediaRecorder 기본은 webm/opus. 그 외 일부 매핑. 매칭 안 되면 webm 으로 가정. */
  private static String mapEncoding(String contentType) {
    if (contentType == null) return "WEBM_OPUS";
    String ct = contentType.toLowerCase();
    if (ct.contains("webm")) return "WEBM_OPUS";
    if (ct.contains("ogg")) return "OGG_OPUS";
    if (ct.contains("mp3") || ct.contains("mpeg")) return "MP3";
    if (ct.contains("wav") || ct.contains("x-wav")) return "LINEAR16";
    if (ct.contains("flac")) return "FLAC";
    return "WEBM_OPUS";
  }

  /** 응답: {"results": [{"alternatives": [{"transcript": "..."}]}]}. 결과 없으면 빈 문자열. */
  private String extractTranscript(Map<String, Object> res) {
    if (res == null) return "";
    Object resultsObj = res.get("results");
    if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
      log.debug("google stt: no results");
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Object item : results) {
      if (!(item instanceof Map<?, ?> result)) continue;
      Object altObj = result.get("alternatives");
      if (!(altObj instanceof List<?> alts) || alts.isEmpty()) continue;
      Object first = alts.get(0);
      if (!(first instanceof Map<?, ?> alt)) continue;
      Object text = alt.get("transcript");
      if (text instanceof String s) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(s.trim());
      }
    }
    return sb.toString();
  }
}
