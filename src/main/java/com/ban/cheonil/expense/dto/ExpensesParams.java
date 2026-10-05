package com.ban.cheonil.expense.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * GET /expenses 파라미터 — 기간 범위 + 카테고리 / 매장 / 지출명 필터. 전체 응답 · 클라 페이징 (UI 가드: 90일 이내).
 *
 * @param ctgSeq 카테고리 — 하위 카테고리 지출까지 포함
 * @param q 지출명 부분 일치
 */
public record ExpensesParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    Integer ctgSeq,
    Short storeSeq,
    String q) {}
