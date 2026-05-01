package com.ban.cheonil.sales.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.payment.entity.PayType;

/**
 * 거래 내역 row — t_order + m_store + t_order_menu + m_menu + t_payment join 결과.
 *
 * <ul>
 *   <li>{@code menuSummary} — "제육 1, 돈까스 1" 형태 (서버에서 join + 요약)
 *   <li>{@code payType} null = 미수 (결제 row 없음)
 *   <li>{@code payAmount} 미수면 0
 * </ul>
 */
public record TransactionRes(
    Long orderSeq,
    Short storeSeq,
    String storeNm,
    String menuSummary,
    Integer orderAmount,
    OffsetDateTime orderAt,
    OffsetDateTime cookedAt,
    PayType payType,
    OffsetDateTime payAt,
    Integer payAmount) {}
