package com.ban.cheonil.sales.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.payment.entity.PayType;

/** 거래 row 의 결제 entry — 분할 결제 시 여러 entry. */
public record PaymentRes(PayType payType, Integer amount, OffsetDateTime payAt) {}
