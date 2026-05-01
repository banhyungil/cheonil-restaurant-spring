package com.ban.cheonil.sales.dto;

/**
 * GET /sales/unpaid 파라미터 — 수금 탭 전용 (날짜 무관 모든 미수).
 *
 * <p>정산 탭의 transactions 와 분리한 이유: 의미 명확 (date 필요 X) + 수금은 누적 미수 일괄 처리 시나리오.
 */
public record UnpaidParams(Short storeSeq, Integer page, Integer size) {}
