package com.ban.cheonil.sales.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** GET /sales/summary 파라미터 — 단일 날짜 (정산은 통계가 아닌 결제 정리에 집중). */
public record SalesSummaryParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {}
