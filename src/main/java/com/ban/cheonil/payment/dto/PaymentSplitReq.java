package com.ban.cheonil.payment.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 단일 주문 분할 결제 페이로드.
 *
 * <p>{@code splits} 의 amount 합계 === 주문금액 검증은 서버 책임 (트랜잭션 내).
 */
public record PaymentSplitReq(
    @NotNull Long orderSeq, @NotEmpty @Valid List<PaymentSplitItem> splits) {}
