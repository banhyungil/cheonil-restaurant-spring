package com.ban.cheonil.sales.stats.dto;

/** 결제유형 비율 — 도넛 1조각. {@code payType} = "CASH" / "CARD" / "UNPAID". {@code percent} 0~100. */
public record PayMethodPart(String payType, Integer amount, Double percent) {}
