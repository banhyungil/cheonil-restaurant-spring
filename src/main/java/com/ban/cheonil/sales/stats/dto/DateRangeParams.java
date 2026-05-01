package com.ban.cheonil.sales.stats.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** 통계 공통 — 날짜 범위. */
public record DateRangeParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {}
