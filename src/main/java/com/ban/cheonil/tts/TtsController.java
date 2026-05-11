package com.ban.cheonil.tts;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.tts.TtsService.Result;

import lombok.RequiredArgsConstructor;

/**
 * 텍스트 → 음성(mp3) HTTP 엔드포인트.
 *
 * <p>GET 채택 이유: query string 자체가 캐시 키 후보(URL 캐싱), 브라우저 {@code new Audio(url)} 가 직접 stream 받을 수 있어
 * 클라이언트 단순화. 짧은 알림 텍스트(<200자) 가정.
 */
@RestController
@RequestMapping("/tts")
@RequiredArgsConstructor
public class TtsController {

  private final TtsService ttsService;

  @GetMapping
  public ResponseEntity<byte[]> synthesize(
      @RequestParam String text,
      @RequestParam(defaultValue = "1.0") double speed,
      @RequestParam(defaultValue = "0") int gainDb,
      @RequestParam(required = false) String voice) {

    if (text == null || text.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    if (text.length() > 500) {
      return ResponseEntity.badRequest().build();
    }
    if (gainDb < 0 || gainDb > 12) {
      return ResponseEntity.badRequest().build();
    }

    Result r = ttsService.synthesize(text, speed, gainDb, voice);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
    headers.setContentLength(r.bytes().length);
    headers.setCacheControl("public, max-age=86400");
    headers.add("X-Cache", r.cacheHit() ? "HIT" : "MISS");

    return new ResponseEntity<>(r.bytes(), headers, org.springframework.http.HttpStatus.OK);
  }
}
