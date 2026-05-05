package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/**
 * 점포별 메뉴 mix — 매장마다 자체 TOP 5 + 기타.
 *
 * <p>mini donut grid 용 — 매장간 비교가 아닌 매장 특색 (mix 패턴) 분석. 모든 매장에 대해 항상 반환되며 frontend 에서 multi-select
 * 로 보여줄 매장만 필터링.
 */
public record StoreMenuMix(Short storeSeq, String storeNm, List<Item> parts, Integer etcCount) {
  /** 매장 자체 TOP N 메뉴의 한 row. {@code percent} 0~100. */
  public record Item(String menuNm, Integer count, Double percent) {}
}
