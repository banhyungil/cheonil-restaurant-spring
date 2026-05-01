package com.ban.cheonil.sales.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** GET /sales/transactions 파라미터 — 단일 날짜 + payType 필터. payType: 'CASH' / 'CARD' / 'UNPAID' / null(전체). */
public record TransactionsParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    Short storeSeq,
    String payType,
    Integer page,
    Integer size) {}
