package com.ban.cheonil.sales.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** GET /sales/transactions 파라미터 — 단일 날짜 (전체 응답, 클라 페이징/필터). */
public record TransactionsParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    Short storeSeq) {}
