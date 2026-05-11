package com.ban.cheonil.tts;

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
 * MeloTTS (자체 호스팅) 프록시 + 디스크 캐시.
 *
 * <p>흐름: key 계산 → 캐시 hit ? 즉시 반환 : MeloTTS 호출 → 캐시 저장 → 반환.
 *
 * <p>{@link com.ban.cheonil.speech.SpeechService} 의 RestTemplate 패턴을 따른다(Whisper 와 동일 구조).
 */
@Service
@RequiredArgsConstructor
public class TtsService {

  private static final Logger log = LoggerFactory.getLogger(TtsService.class);

  private final RestTemplate restTemplate = new RestTemplate();
  private final TtsCacheService cache;

  @Value("${melotts.url}")
  private String melottsUrl;

  public record Result(byte[] bytes, boolean cacheHit) {}

  public Result synthesize(String text, double speed, int gainDb) {
    String key = cache.keyOf(text, speed, gainDb);

    byte[] cached = cache.get(key);
    if (cached != null) {
      log.debug("tts cache hit key={} len={}", key.substring(0, 8), text.length());
      return new Result(cached, true);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> req =
        new HttpEntity<>(
            Map.of("text", text, "speed", speed, "gain_db", gainDb, "format", "mp3"), headers);

    byte[] bytes = restTemplate.postForObject(melottsUrl + "/tts", req, byte[].class);
    if (bytes == null || bytes.length == 0) {
      throw new IllegalStateException("MeloTTS returned empty response");
    }

    cache.put(key, bytes);
    log.debug("tts cache miss key={} len={} bytes={}", key.substring(0, 8), text.length(), bytes.length);
    return new Result(bytes, false);
  }
}
