package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/**
 * 시간대×매장 heatmap row — 매장 1개의 시간대별 주문 건수.
 *
 * <p>모든 매장이 동일한 hour bucket 셋을 가짐 (백엔드가 9~20 시간대 0 채움 보장).
 */
public record StoreHourHeatmap(Short storeSeq, String storeNm, List<HourCount> hourly) {
  /** 한 시간대의 주문 건수. */
  public record HourCount(Integer hour, Integer count) {}
}
