package com.ban.cheonil.voiceOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.voiceOrder.dto.VoiceOrderLogRes;
import com.ban.cheonil.voiceOrder.entity.VoiceOrderLog;

import lombok.RequiredArgsConstructor;

/**
 * 운영자용 음성 주문 로그 조회.
 *
 * <ul>
 *   <li>{@code GET /voice-order-logs?page=&size=} — 최근순 페이지네이션 리스트
 *   <li>{@code GET /voice-order-logs/{seq}/audio} — 원본 오디오 stream (분쟁 검증/재생)
 * </ul>
 */
@RestController
@RequestMapping("/voice-order-logs")
@RequiredArgsConstructor
public class VoiceOrderLogController {

  /** 페이지 사이즈 상한 — 과도한 요청 방지. */
  private static final int MAX_PAGE_SIZE = 100;

  private final VoiceOrderLogRepo repo;
  private final VoiceOrderLogService logService;

  @GetMapping
  public Page<VoiceOrderLogRes> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
    return repo.findAllByOrderByCreatedAtDesc(pageable).map(VoiceOrderLogRes::from);
  }

  /** 원본 오디오 stream — Content-Type 은 저장 시점의 audio_mime 그대로. */
  @GetMapping("/{seq}/audio")
  public ResponseEntity<ByteArrayResource> audio(@PathVariable Long seq) throws IOException {
    VoiceOrderLog log =
        repo.findById(seq).orElseThrow(() -> new EntityNotFoundException("log " + seq));

    Path path = logService.absolutePath(log.getAudioPath());
    if (!Files.isRegularFile(path)) {
      // retention cleanup 후 file 만 사라진 케이스 — row 는 남아있을 수 있음.
      throw new EntityNotFoundException("audio file missing for log " + seq);
    }
    byte[] bytes = Files.readAllBytes(path);
    ByteArrayResource body = new ByteArrayResource(bytes);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(log.getAudioMime()));
    headers.setContentLength(bytes.length);
    // 운영자 페이지에서 <audio> 가 캐싱하도록 — content 는 immutable.
    headers.setCacheControl("private, max-age=3600");

    return ResponseEntity.ok().headers(headers).body(body);
  }
}
