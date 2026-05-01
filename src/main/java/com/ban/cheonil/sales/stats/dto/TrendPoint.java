package com.ban.cheonil.sales.stats.dto;

/** 매출 추이 차트의 한 점 — granularity 단위 라벨 + 합계. */
public record TrendPoint(String label, Integer amount) {}
