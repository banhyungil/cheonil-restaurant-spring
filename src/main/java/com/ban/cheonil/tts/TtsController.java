package com.ban.cheonil.tts;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.tts.TtsCacheService.CacheEntry;
import com.ban.cheonil.tts.TtsService.Result;
import com.ban.cheonil.tts.dto.TtsCacheDeleteReq;

import lombok.RequiredArgsConstructor;

/**
 * 텍스트 → 음성(mp3) HTTP 엔드포인트 + 캐시 관리(TTS 음성 관리 화면).
 *
 * <p>GET 채택 이유: query string 자체가 캐시 키 후보(URL 캐싱), 브라우저 {@code new Audio(url)} 가 직접 stream 받을 수 있어
 * 클라이언트 단순화. 짧은 알림 텍스트(<200자) 가정.
 */
@RestController
@RequestMapping("/tts")
@RequiredArgsConstructor
public class TtsController {

  private final TtsService ttsService;
  private final TtsCacheService cache;

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

  /**
   * 캐시 목록 — 최근 사용순 전체. 페이지네이션 없음 (상한 200MB, 항목당 수십 KB 라 최대 수천 건 규모이고 관리 화면 전용).
   *
   * <p>메타데이터 sidecar 가 없는 구 캐시 파일은 제외된다 — 재사용되는 시점에 {@code
   * backfillMetaIfMissing} 로 편입된다.
   */
  @GetMapping("/cache")
  public List<CacheEntry> cacheList() {
    return cache.list();
  }

  /** 단건 삭제 — 다음 발화 때 같은 파라미터로 재합성된다. */
  @DeleteMapping("/cache/{key}")
  public ResponseEntity<Void> deleteCache(@PathVariable String key) {
    return cache.delete(key)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  /** 다중 삭제 — 관리 화면에서 체크한 항목들. 일부가 이미 없어도 나머지는 지운다. */
  @DeleteMapping("/cache")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCaches(@Valid @RequestBody TtsCacheDeleteReq req) {
    cache.deleteAll(req.keys());
  }
}
