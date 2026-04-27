package com.ban.cheonil.orderRsv.dto;

import com.ban.cheonil.orderRsv.entity.RsvStatus;

/** PATCH /order-rsvs/{seq}/status body. */
public record OrderRsvStatusChangeReq(RsvStatus status) {}
