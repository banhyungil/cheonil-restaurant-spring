package com.ban.cheonil.sales.stats.dto;

/** 메뉴 ranking row — TOP N 차트 / list 공용. */
public record MenuRank(String menuNm, Integer count, Integer amount) {}
