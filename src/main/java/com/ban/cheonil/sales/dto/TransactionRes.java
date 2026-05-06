package com.ban.cheonil.sales.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 거래 내역 row — t_order + m_store + t_order_menu + m_menu + t_payment join 결과.
 *
 * <ul>
 *   <li>{@code menuSummary} — "제육 1, 돈까스 1" 형태 (서버에서 join + 요약)
 *   <li>{@code payments} — 결제 entry 목록. 비어 있으면 미수. 분할 결제는 다수 entry.
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
    List<PaymentRes> payments) {}
