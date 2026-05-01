package com.ban.cheonil.sales.dto;

/**
 * 그리드 탭 KPI 4 카드 응답 — 기간 집계.
 *
 * <p>정산의 {@link SalesSummaryRes} 와 다름: 단일 날짜가 아닌 기간 집계 + prevSales/netSales/unpaid 없음.
 */
public record OrdersSummaryRes(
    Integer totalSales,
    Integer totalCount,
    Integer avgDailySales,
    Integer avgDailyCount,
    PayMethodSummary cash,
    PayMethodSummary card) {}
