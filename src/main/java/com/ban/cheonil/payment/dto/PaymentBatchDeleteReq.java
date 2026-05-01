package com.ban.cheonil.payment.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * 결제 일괄 취소 페이로드 — 주문 단위.
 *
 * <p>분할 결제 케이스에서 한 주문에 묶인 t_payment 들을 일괄 처리하기 위해 paymentSeq 가 아닌 orderSeq 로 받는다. 단건 취소도 동일
 * 엔드포인트 ({@code orderSeqs: [seq]}) 사용 — UI 수금 탭의 단건/다건 통합.
 */
public record PaymentBatchDeleteReq(@NotEmpty List<Long> orderSeqs) {}
