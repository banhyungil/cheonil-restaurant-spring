package com.ban.cheonil.orderRsv.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.orderRsv.entity.RsvStatus;

/** PATCH /order-rsvs/{seq}/status 응답 — 변경된 핵심 필드만. */
public record OrderRsvStatusChangeRes(Long seq, RsvStatus status, OffsetDateTime modAt) {}
