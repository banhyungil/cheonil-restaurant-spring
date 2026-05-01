package com.ban.cheonil.sales.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * GET /sales/orders, /sales/orders/summary 파라미터.
 *
 * <p>그리드 탭 — 기간 범위 + 매장/메뉴/결제수단 필터. UI 가드: 90일 초과 호출 금지.
 */
public record OrdersParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    Short storeSeq,
    Short menuSeq,
    String payType) {}
