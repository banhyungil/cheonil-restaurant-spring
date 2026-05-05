package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/** 통계 - 점포 분석 뷰 응답. */
public record StatsStoreRes(
    List<StoreSales> stores,
    List<StoreMenuMix> storeMenuMixes,
    List<StoreCount> orderCounts,
    List<StoreUnpaid> unpaidByStore,
    List<StorePayDistribution> payDistribution) {}
