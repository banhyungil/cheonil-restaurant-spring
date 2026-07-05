package com.ban.cheonil.sales.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** GET /sales/transactions 파라미터 — 날짜 범위(from~to) (전체 응답, 클라 페이징/필터). */
public record TransactionsParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    Short storeSeq) {}
