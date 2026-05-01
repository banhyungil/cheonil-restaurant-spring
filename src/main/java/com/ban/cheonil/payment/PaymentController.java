package com.ban.cheonil.payment;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.payment.dto.PaymentCreateReq;
import com.ban.cheonil.payment.dto.PaymentRes;
import com.ban.cheonil.payment.dto.PaymentSplitReq;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
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

  /** 단건 결제 취소. */
  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Long seq) {
    paymentService.remove(seq);
  }

  /** 다중 일괄 취소 — {@code ?seqs=1,2,3} 로 받음 (axios qs arrayFormat=comma). */
  @DeleteMapping("/batch")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeBatch(@RequestParam List<Long> seqs) {
    paymentService.removeBatch(seqs);
  }
}
