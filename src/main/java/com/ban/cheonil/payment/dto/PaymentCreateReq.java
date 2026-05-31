package com.ban.cheonil.payment.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.payment.entity.PayType;

import jakarta.validation.constraints.NotNull;

/**
 * 단건 결제 페이로드. 금액은 서버가 t_order.amount 에서 가져와 채움 — 클라이언트 신뢰 X.
 *
 * <p>{@code payAt} = 수금 일시. 수금을 나중에 입력하는 경우 과거 일시 지정 가능. null 이면 서버 현재시각.
 */
public record PaymentCreateReq(
    @NotNull Long orderSeq, @NotNull PayType payType, OffsetDateTime payAt) {}
