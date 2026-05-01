package com.ban.cheonil.payment;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.payment.dto.PaymentBatchDeleteReq;
import com.ban.cheonil.payment.dto.PaymentCreateReq;
import com.ban.cheonil.payment.dto.PaymentRes;
import com.ban.cheonil.payment.dto.PaymentSplitReq;

import lombok.RequiredArgsConstructor;

/** 결제는 주문의 sub-resource 로 {@code /orders/payments} prefix 사용. */
@RestController
@RequestMapping("/orders/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  /** 단건 결제 — 주문 상태를 PAID 로 전환. */
  @PostMapping
  public ResponseEntity<PaymentRes> create(@Valid @RequestBody PaymentCreateReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(req));
  }

  /** 다중 일괄 결제 — 수금 탭 [현금]/[카드] 버튼. */
  @PostMapping("/batch")
  public ResponseEntity<List<PaymentRes>> createBatch(
      @Valid @RequestBody List<PaymentCreateReq> reqs) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createBatch(reqs));
  }

  /** 단일 주문 분할 결제 — splits 합계 = 주문금액 검증. */
  @PostMapping("/split")
  public ResponseEntity<List<PaymentRes>> createSplit(@Valid @RequestBody PaymentSplitReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createSplit(req));
  }

  /**
   * 결제 일괄 취소 — 주문 단위 (단건도 {@code orderSeqs: [seq]} 사용).
   *
   * <p>POST + body 인 이유: DELETE 는 일반적으로 body 사용을 지양하며 query string 의 배열 직렬화도 클라이언트마다 다름. POST + body
   * 가 호환성 + 분할 결제 안전성 (한 주문의 모든 row 일괄) 측면에서 더 명확.
   */
  @PostMapping("/batch-delete")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void batchDelete(@Valid @RequestBody PaymentBatchDeleteReq req) {
    paymentService.removeByOrderSeqs(req.orderSeqs());
  }
}
