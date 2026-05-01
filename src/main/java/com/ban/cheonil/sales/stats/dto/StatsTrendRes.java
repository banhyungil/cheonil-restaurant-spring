package com.ban.cheonil.sales.stats.dto;

import java.util.List;

/** 매출 추이 차트 응답 — 차트 로컬 granularity segment 변경 시만 호출. */
public record StatsTrendRes(
    StatsGranularity granularity, List<TrendPoint> trend, List<TrendPoint> trendPrev) {}
