package com.ban.cheonil.voiceOrder.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.voiceOrder.entity.VoiceOrderLog;

/**
 * 음성 주문 로그 응답 — 운영자 페이지 표시용.
 *
 * <p>audio_path 는 보안상 직접 노출 X — 오디오 재생은 별도 endpoint ({@code
 * GET /api/voice-order-logs/{seq}/audio}) 로 stream.
 */
public record VoiceOrderLogRes(
    Long seq,
    Long orderSeq,
    String engineUsed,
    String whisperText,
    String googleText,
    String finalText,
    String errorMessage,
    String audioMime,
    Integer audioSizeBytes,
    OffsetDateTime createdAt) {

  public static VoiceOrderLogRes from(VoiceOrderLog e) {
    return new VoiceOrderLogRes(
        e.getSeq(),
        e.getOrderSeq(),
        e.getEngineUsed(),
        e.getWhisperText(),
        e.getGoogleText(),
        e.getFinalText(),
        e.getErrorMessage(),
        e.getAudioMime(),
        e.getAudioSizeBytes(),
        e.getCreatedAt());
  }
}
