package com.ban.cheonil.sales.stats.dto;

/** 점포별 주문 건수. */
public record StoreCount(Short storeSeq, String storeNm, Integer count) {}
