package com.ban.cheonil.sales.stats.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** 매출 추이 차트 전용 — granularity 추가. */
public record StatsTrendParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @NotNull StatsGranularity granularity) {}
