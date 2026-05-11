package com.ban.cheonil.voiceOrder.dto;

import java.util.List;

/**
 * 음성/텍스트 주문 파싱 결과.
 *
 * @param storeSeq 매장 seq — 발화에 매장 명시된 경우만, 없으면 null
 * @param menus 파싱된 메뉴 + 수량 목록 (OrderCreateReq.menus 와 명명 일치)
 * @param cmt 비고 (덜맵게 / 양배추 빼고 등) — 없으면 null (OrderCreateReq.cmt 와 명명 일치)
 * @param unmatched LLM 이 매장/메뉴 사전에서 매칭하지 못한 발화 조각 (원문 그대로). 직접 주문 생성 시
 *     비어있어야 함 — 비어있지 않으면 검증 실패로 차단.
 * @param raw 디버깅용 — claude 가 반환한 원본 JSON 텍스트
 */
public record VoiceOrderRes(
    Short storeSeq,
    List<VoiceOrderItem> menus,
    String cmt,
    List<String> unmatched,
    String raw) {}
