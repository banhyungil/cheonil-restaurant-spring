package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/**
 * 통계 - 메뉴 분석 뷰 응답.
 *
 * <ul>
 *   <li>{@code menusTop10} — 수량 기준 TOP 10
 *   <li>{@code menusTop10ByAmount} — 판매액 기준 TOP 10 ("잘 팔리는" vs "매출 큰" 메뉴 비교)
 *   <li>{@code categoryParts} — 카테고리별 매출 도넛
 *   <li>{@code hourlyMenuStack} — 시간대별 메뉴 판매 stacked (TOP 5 + 기타)
 * </ul>
 */
public record StatsMenuRes(
    List<MenuRank> menusTop10,
    List<MenuRank> menusTop10ByAmount,
    List<CategoryPart> categoryParts,
    StatsHourMenuStack hourlyMenuStack) {}
