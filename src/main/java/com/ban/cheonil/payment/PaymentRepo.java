package com.ban.cheonil.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.payment.entity.Payment;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

  /** 단일 주문에 연결된 결제 row 들 — 분할 결제 케이스 포함. 일괄 취소의 단위 fetch 도 겸함. */
  List<Payment> findByOrderSeq(Long orderSeq);

  /** 거래 내역 page 조립 — N 주문에 대해 한번에 fetch. */
  List<Payment> findByOrderSeqIn(List<Long> orderSeqs);
}
