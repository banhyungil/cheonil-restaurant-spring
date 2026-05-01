package com.ban.cheonil.sales.stats.dto;

/** 카테고리별 매출 도넛 한 조각. {@code percent} 0~100. */
public record CategoryPart(String ctgNm, Double percent, Integer amount) {}
