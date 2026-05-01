package com.ban.cheonil.sales.stats.dto;

/** 점포별 매출 ranking. */
public record StoreSales(Short storeSeq, String storeNm, Integer amount) {}
