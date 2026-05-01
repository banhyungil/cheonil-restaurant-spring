package com.ban.cheonil.payment.dto;

import com.ban.cheonil.payment.entity.PayType;

import jakarta.validation.constraints.NotNull;

/**
 * 단건 결제 페이로드. 금액은 서버가 t_order.amount 에서 가져와 채움 — 클라이언트 신뢰 X.
 */
public record PaymentCreateReq(@NotNull Long orderSeq, @NotNull PayType payType) {}
