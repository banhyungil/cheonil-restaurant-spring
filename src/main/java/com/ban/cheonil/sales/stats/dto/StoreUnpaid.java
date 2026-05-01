package com.ban.cheonil.sales.stats.dto;

/** 점포별 미수 — 미수 현황 카드. */
public record StoreUnpaid(Short storeSeq, String storeNm, Integer amount, Integer count) {}
