package com.ban.cheonil.voiceOrder.dto;

import java.util.List;

/**
 * 음성/텍스트 주문 파싱 결과.
 *
 * @param storeSeq 매장 seq — 발화에 매장 명시된 경우만, 없으면 null
 * @param items    파싱된 메뉴 + 수량 목록
 * @param memo     비고 (덜맵게 / 양배추 빼고 등) — 없으면 null
 * @param raw      디버깅용 — claude 가 반환한 원본 JSON 텍스트
 */
public record VoiceOrderRes(
    Short storeSeq, List<VoiceOrderItem> items, String memo, String raw) {}
