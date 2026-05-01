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
   * Delete (cancel)
   * ========================================================= */

  /**
   * 단건 결제 취소.
   *
   * <p>분할 결제의 경우 한 row 만 취소되면 t_order.amount > sum(t_payment.amount) 가 되어 부분 미수 상태가 되므로, 분할 결제 row 가
   * 있는 주문은 모든 row 를 함께 취소해야 함 → {@link #removeBatch} 사용 권장. 본 메서드는 단일 결제 row 가 있는 주문 또는 동일한 결과를
   * 의도한 단순 취소만 처리.
   */
  @Transactional
  public void remove(Long seq) {
    Payment p = getPayment(seq);
    Long orderSeq = p.getOrderSeq();
    paymentRepo.delete(p);

    // 같은 주문에 다른 결제 row 가 남아있다면 status 는 PAID 유지, 모두 사라졌으면 COOKED 복귀.
    if (paymentRepo.findByOrderSeq(orderSeq).isEmpty()) {
      orderService.revertToCookedFromPaid(orderSeq);
    }
  }

  /** 다중 일괄 취소. */
  @Transactional
  public void removeBatch(List<Long> seqs) {
    List<Payment> payments = paymentRepo.findBySeqIn(seqs);
    if (payments.size() != seqs.size()) {
      throw new EntityNotFoundException("일부 결제를 찾을 수 없음 (요청: " + seqs.size() + ", 조회: " + payments.size() + ")");
    }
    paymentRepo.deleteAll(payments);

    // 각 주문에 대해 남은 결제 확인 후 status 복귀
    payments.stream()
        .map(Payment::getOrderSeq)
        .distinct()
        .forEach(
            orderSeq -> {
              if (paymentRepo.findByOrderSeq(orderSeq).isEmpty()) {
                orderService.revertToCookedFromPaid(orderSeq);
              }
            });
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  private Order getOrder(Long seq) {
    return orderRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
  }

  private Payment getPayment(Long seq) {
    return paymentRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("payment " + seq + " not found"));
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
