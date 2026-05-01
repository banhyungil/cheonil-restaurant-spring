package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/** 통계 - 메뉴 분석 뷰 응답. */
public record StatsMenuRes(
    List<MenuRank> menusTop10,
    List<CategoryPart> categoryParts,
    List<MenuRank> popularByCash,
    List<MenuRank> popularByCard,
    List<MenuRank> peakTimeMenus) {}
