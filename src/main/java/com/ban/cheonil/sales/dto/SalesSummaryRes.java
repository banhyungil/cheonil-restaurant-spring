package com.ban.cheonil.sales.dto;

import java.time.LocalDate;

/**
 * 정산 KPI 5 카드 응답 — 단일 날짜 기준.
 *
 * <p>UI 카드:
 *
 * <ol>
 *   <li>매출 + 전일 대비 증감% (frontend 가 {@code prevSales} 로 계산)
 *   <li>순매출 (totalSales - expenseTotal)
 *   <li>💵 현금
 *   <li>💳 카드
 *   <li>⚠ 미수 (그날 미수만 — 수금 탭의 전체 미수와 구별)
 * </ol>
 *
 * <p>기간 통계 / 트렌드는 주문내역 페이지가 담당. 정산은 일자별 결제 정리에 집중.
 */
public record SalesSummaryRes(
    LocalDate date,
    Integer totalSales,
    Integer prevSales,
    Integer netSales,
    Integer expenseTotal,
    PayMethodSummary cash,
    PayMethodSummary card,
    PayMethodSummary unpaid) {}
