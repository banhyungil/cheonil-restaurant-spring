package com.ban.cheonil.payment;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.order.OrderRepo;
import com.ban.cheonil.order.OrderService;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderStatus;
import com.ban.cheonil.payment.dto.PaymentCreateReq;
import com.ban.cheonil.payment.dto.PaymentRes;
import com.ban.cheonil.payment.dto.PaymentSplitItem;
import com.ban.cheonil.payment.dto.PaymentSplitReq;
import com.ban.cheonil.payment.entity.Payment;

import lombok.RequiredArgsConstructor;

/**
 * 결제 서비스.
 *
 * <ul>
 *   <li><b>단건 결제</b>: t_payment INSERT + t_order.status=PAID
 *   <li><b>다중 일괄</b>: 단건 결제를 N개 주문에 적용
 *   <li><b>분할 결제</b>: 단일 주문에 여러 t_payment row, 합계 = order.amount 검증
 *   <li><b>결제 취소</b>: t_payment DELETE + t_order.status=COOKED 복귀 (PAID 주문 한정)
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepo paymentRepo;
  private final OrderRepo orderRepo;
  private final OrderService orderService;

  /* =========================================================
   * Create
   * ========================================================= */

  /** 단건 결제 — 주문 amount 전체를 한 결제수단으로 처리. */
  @Transactional
  public PaymentRes create(PaymentCreateReq req) {
    Order order = getOrder(req.orderSeq());
    ensurePayable(order);

    Payment p = saveOnePayment(order.getSeq(), order.getAmount(), req.payType());
    orderService.changeStatus(order.getSeq(), OrderStatus.PAID);
    return PaymentRes.from(p);
  }

  /** 다중 일괄 — 각 주문 별로 단건 결제. 트랜잭션 일부 실패 시 전체 롤백. */
  @Transactional
  public List<PaymentRes> createBatch(List<PaymentCreateReq> reqs) {
    return reqs.stream().map(this::create).toList();
  }

  /**
   * 단일 주문 분할 결제 — 여러 결제수단으로 나눠 t_payment N row INSERT.
   *
   * <p>splits 의 amount 합계가 주문금액과 정확히 일치해야 함. 일치하지 않으면 IllegalArgumentException → 400.
   */
  @Transactional
  public List<PaymentRes> createSplit(PaymentSplitReq req) {
    Order order = getOrder(req.orderSeq());
    ensurePayable(order);

    int sum = req.splits().stream().mapToInt(PaymentSplitItem::amount).sum();
    if (sum != order.getAmount()) {
      throw new IllegalArgumentException(
          "분할 결제 합계가 주문금액과 다릅니다. (합계: " + sum + ", 주문: " + order.getAmount() + ")");
    }

    List<Payment> saved =
        req.splits().stream()
            .map(s -> saveOnePayment(order.getSeq(), s.amount(), s.payType()))
            .toList();
    orderService.changeStatus(order.getSeq(), OrderStatus.PAID);
    return saved.stream().map(PaymentRes::from).toList();
  }

  /* =========================================================
   * Delete (cancel) — 주문 단위 일괄
   * ========================================================= */

  /**
   * 주문 단위 결제 취소 (단건/다건 통합).
   *
   * <p>한 주문의 모든 t_payment row 를 일괄 삭제 + status PAID → COOKED 복귀. 분할 결제도 안전하게 처리됨 (부분 취소로 amount 불일치
   * 케이스 방지). 단건 취소는 {@code orderSeqs: [seq]} 로 호출.
   */
  @Transactional
  public void removeByOrderSeqs(List<Long> orderSeqs) {
    for (Long orderSeq : orderSeqs) {
      List<Payment> payments = paymentRepo.findByOrderSeq(orderSeq);
      if (payments.isEmpty()) {
        throw new EntityNotFoundException("주문 " + orderSeq + " 에 결제 내역이 없습니다");
      }
      paymentRepo.deleteAll(payments);
      orderService.revertToCookedFromPaid(orderSeq);
    }
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  private Order getOrder(Long seq) {
    return orderRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
  }

  private void ensurePayable(Order order) {
    if (order.getStatus() == OrderStatus.PAID) {
      throw new IllegalStateException("이미 결제된 주문입니다 (orderSeq: " + order.getSeq() + ")");
    }
  }

  private Payment saveOnePayment(
      Long orderSeq, Integer amount, com.ban.cheonil.payment.entity.PayType payType) {
    Payment p = new Payment();
    p.setOrderSeq(orderSeq);
    p.setAmount(amount);
    p.setPayType(payType);
    p.setPayAt(OffsetDateTime.now());
    return paymentRepo.save(p);
  }
}
