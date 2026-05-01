package com.ban.cheonil.payment.dto;

import com.ban.cheonil.payment.entity.PayType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 분할 결제 1건의 결제수단 + 금액. */
public record PaymentSplitItem(@NotNull PayType payType, @NotNull @Positive Integer amount) {}
