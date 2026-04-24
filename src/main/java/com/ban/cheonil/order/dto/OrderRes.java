package com.ban.cheonil.order.dto;

import com.ban.cheonil.order.entity.OrderStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderRes(
        Long seq,
        Short storeSeq,
        Integer amount,
        OrderStatus status,
        OffsetDateTime orderAt,
        OffsetDateTime cookedAt,
        String cmt,
        List<OrderItemRes> items
) {
}
