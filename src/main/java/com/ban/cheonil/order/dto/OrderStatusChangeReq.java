package com.ban.cheonil.order.dto;

import com.ban.cheonil.order.entity.OrderStatus;

/** PATCH /orders/{seq}/status 의 body. */
public record OrderStatusChangeReq(OrderStatus status) {
}
