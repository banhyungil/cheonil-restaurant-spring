package com.ban.cheonil.sales.stats.dto;

/** 점포별 결제방식 분포 — stacked bar. */
public record StorePayDistribution(
    Short storeSeq, String storeNm, Integer cash, Integer card, Integer unpaid) {}
