package com.ban.cheonil.voiceOrder;

import com.ban.cheonil.voiceOrder.dto.VoiceOrderRes;

/**
 * 음성 주문 검증 실패 — 발화에서 매장/메뉴를 신뢰 가능한 수준으로 추출하지 못함.
 *
 * <p>{@link Code} 로 어느 게이트가 실패했는지 구분 → 프론트가 사용자 친화 멘트로 안내 (TTS).
 *
 * <p>parsed 는 디버깅용 — 응답 body 에 transcribedText 와 함께 노출되어 어떤 발화가 어떻게 해석됐는지 표시.
 */
public class VoiceOrderValidationException extends RuntimeException {

  public enum Code {
    /** 매장 미지정 — 발화에 매장명 없음. */
    STORE_NOT_MATCHED("매장명 인식 실패. 다시 말씀해주세요"),
    /** 메뉴 0개 — 주문할 항목 없음. */
    NO_ITEMS("주문 메뉴 인식 실패. 다시 말씀해주세요"),
    /** LLM 이 매칭 못한 조각 존재 — 사전에 없는 항목. */
    UNMATCHED_FRAGMENTS("일부 항목을 인식하지 못했습니다. 다시 말씀해주세요"),
    /** 메뉴 seq 가 활성 메뉴 사전에 없음 — LLM hallucination 추정. */
    INVALID_MENU("메뉴 인식 오류. 다시 말씀해주세요"),
    /** 수량이 0 또는 음수. */
    INVALID_QUANTITY("수량이 올바르지 않습니다");

    private final String message;

    Code(String message) {
      this.message = message;
    }

    public String message() {
      return message;
    }
  }

  private final Code code;
  private final String transcribedText;
  private final VoiceOrderRes parsed;

  public VoiceOrderValidationException(Code code, String transcribedText, VoiceOrderRes parsed) {
    super(code.message());
    this.code = code;
    this.transcribedText = transcribedText;
    this.parsed = parsed;
  }

  public Code code() {
    return code;
  }

  public String transcribedText() {
    return transcribedText;
  }

  public VoiceOrderRes parsed() {
    return parsed;
  }
}
