package com.ban.cheonil.order.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.order.entity.OrderStatus;

/**
 * PATCH /orders/{seq}/status 응답 — 상태 전이로 변경된 필드만 포함.
 *
 * <p>전체 aggregate 대신 변경 결과만 내려서 payload 최소화.
 * 클라이언트는 이 값으로 캐시 부분 patch 또는 단순 확인용으로 사용.
 */
public record OrderStatusChangeRes(
    Long seq,
    OrderStatus status,
    OffsetDateTime cookedAt,
    OffsetDateTime modAt) {}
