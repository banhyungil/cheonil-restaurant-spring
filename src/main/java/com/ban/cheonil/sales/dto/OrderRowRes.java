package com.ban.cheonil.sales.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.order.entity.OrderStatus;
import com.ban.cheonil.payment.entity.PayType;

/**
 * 주문내역관리 그리드 row — {@link TransactionRes} 와 동일한 join 결과 + status / cmt 추가.
 *
 * <p>정산 페이지의 read-only 거래 내역과 달리 status / 비고 (cmt) 노출.
 */
public record OrderRowRes(
    Long orderSeq,
    Short storeSeq,
    String storeNm,
    String menuSummary,
    Integer orderAmount,
    OffsetDateTime orderAt,
    OffsetDateTime cookedAt,
    PayType payType,
    OffsetDateTime payAt,
    Integer payAmount,
    OrderStatus status,
    String cmt) {}
