package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/** 통계 - 기본 뷰 응답. granularity 무관 (단순 집계). */
public record StatsBasicRes(
    Integer totalSales,
    Integer prevSales,
    Integer totalCount,
    Integer prevCount,
    List<HourBucket> hourly,
    List<StoreSales> storesTop5,
    List<PayMethodPart> payParts,
    List<MenuRank> menusTop5) {}
