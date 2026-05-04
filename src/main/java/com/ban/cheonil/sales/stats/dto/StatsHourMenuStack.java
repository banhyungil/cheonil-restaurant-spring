package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/**
 * 시간대별 메뉴 판매 stacked — 메뉴 분석 뷰 (시간 × 메뉴 cross-tab).
 *
 * <p>{@code menus} 는 TOP 5 메뉴명 + 마지막 "기타" (총 6개). 모든 hour 에 대해 동일 순서/길이. {@code hours[].counts.length
 * === menus.length} 보장 (해당 시간 미판매면 0).
 */
public record StatsHourMenuStack(List<String> menus, List<HourMenuStack> hours) {
  /** 한 시간대의 메뉴별 판매 수량. counts 인덱스는 부모 menus 와 동일. */
  public record HourMenuStack(Integer hour, List<Integer> counts) {}
}
