package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/**
 * 점포별 메뉴 비중 — donut + list (TOP 4 + 기타).
 *
 * <p>{@code etcCount} 는 TOP 4 외 합산 건수 — tooltip / footer 표시용.
 */
public record StoreMenuPart(Short storeSeq, List<Item> parts, Integer etcCount) {
  public record Item(String menuNm, Integer count, Double percent) {}
}
