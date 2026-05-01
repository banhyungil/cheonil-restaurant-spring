package com.ban.cheonil.sales.stats.dto;

/** 시간대별 매출 — 09~20시 (영업 시간) bucket. {@code hour} 0~23. */
public record HourBucket(Integer hour, Integer amount) {}
