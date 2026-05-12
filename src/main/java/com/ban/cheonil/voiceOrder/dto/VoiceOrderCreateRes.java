package com.ban.cheonil.voiceOrder.dto;

import com.ban.cheonil.order.dto.OrderExtRes;

/**
 * 음성 주문 생성 결과 — STT + parse + create 통합 endpoint 응답.
 *
 * @param order 생성된 주문 (aggregate)
 * @param transcribedText STT 로 변환된 발화 텍스트 — 화면 표시 + 디버깅용
 * @param confirmation 음성 확인 멘트 (TTS 로 읽어줄 텍스트). 예: "강남점 양념치킨 2, 콜라 1, 덜맵게 — 주문 완료"
 * @param engine 실제로 성공한 STT 엔진 — "WHISPER" 또는 "GOOGLE" (fallback 발생 시 GOOGLE). 디버깅/검증용.
 */
public record VoiceOrderCreateRes(
    OrderExtRes order, String transcribedText, String confirmation, String engine) {}
