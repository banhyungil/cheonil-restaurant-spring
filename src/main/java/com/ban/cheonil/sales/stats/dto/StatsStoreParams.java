package com.ban.cheonil.sales.stats.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/** 점포 분석 — 점포별 메뉴 비중 select 변경 시 storeSeq 추가. */
public record StatsStoreParams(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    Short storeSeq) {}
