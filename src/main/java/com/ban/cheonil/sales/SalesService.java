package com.ban.cheonil.sales;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.entities.ExpenseRepo;
import com.ban.cheonil.order.OrderMenuRepo;
import com.ban.cheonil.order.OrderRepo;
import com.ban.cheonil.order.dto.OrderMenuExtRes;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderMenu;
import com.ban.cheonil.order.entity.OrderStatus;
import com.ban.cheonil.order.sse.OrderEvent;
import com.ban.cheonil.payment.PaymentRepo;
import com.ban.cheonil.payment.entity.PayType;
import com.ban.cheonil.payment.entity.Payment;
import com.ban.cheonil.sales.dto.OrderRowRes;
import com.ban.cheonil.sales.dto.OrdersParams;
import com.ban.cheonil.sales.dto.OrdersSummaryRes;
import com.ban.cheonil.sales.dto.PayMethodSummary;
import com.ban.cheonil.sales.dto.PaymentRes;
import com.ban.cheonil.sales.dto.SalesSummaryParams;
import com.ban.cheonil.sales.dto.SalesSummaryRes;
import com.ban.cheonil.sales.dto.TransactionRes;
import com.ban.cheonil.sales.dto.TransactionsParams;
import com.ban.cheonil.sales.dto.UnpaidParams;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

/**
 * 정산 서비스 — 단일 날짜 KPI + 거래 내역 페이징 + 전체 미수 페이징.
 *
 * <p>설계 원칙:
 *
 * <ul>
 *   <li>정산 = 일자별 결제 정리에 집중. 통계 / 기간 트렌드는 주문내역 페이지가 담당.
 *   <li>수금 탭 미수는 날짜 무관 (운영자가 누적 미수 일괄 수금) → {@code /sales/unpaid} 별도.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesService {

  private final OrderRepo orderRepo;
  private final OrderMenuRepo orderMenuRepo;
  private final PaymentRepo paymentRepo;
  private final ExpenseRepo expenseRepo;
  private final StoreRepo storeRepo;
  private final ApplicationEventPublisher eventPublisher;

  /* =========================================================
   * Summary KPI — 단일 날짜 + 전일 비교
   * ========================================================= */

  public SalesSummaryRes summary(SalesSummaryParams params) {
    LocalDate date = params.date();
    OffsetDateTime[] dayRange = dayRangeOf(date);
    OffsetDateTime[] prevDayRange = dayRangeOf(date.minusDays(1));

    int totalSales = orderRepo.sumAmountByOrderAtRange(dayRange[0], dayRange[1]);
    int prevSales = orderRepo.sumAmountByOrderAtRange(prevDayRange[0], prevDayRange[1]);
    int expenseTotal = expenseRepo.sumAmountByExpenseAtRange(dayRange[0], dayRange[1]);
    int netSales = totalSales - expenseTotal;

    List<Payment> payments = paymentRepo.findByPayAtBetween(dayRange[0], dayRange[1]);
    PayMethodSummary cash = aggregateBy(payments, PayType.CASH);
    PayMethodSummary card = aggregateBy(payments, PayType.CARD);

    // 그날 미수 — 그날 주문 중 status != PAID (수금 탭의 전체 미수와 다름)
    Specification<Order> dayUnpaidSpec =
        Specification.allOf(
            (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("orderAt"), dayRange[0]),
            (r, q, cb) -> cb.lessThan(r.get("orderAt"), dayRange[1]),
            (r, q, cb) -> cb.notEqual(r.get("status"), OrderStatus.PAID));
    List<Order> dayUnpaidOrders = orderRepo.findAll(dayUnpaidSpec);
    int unpaidAmount = dayUnpaidOrders.stream().mapToInt(Order::getAmount).sum();
    PayMethodSummary unpaid = new PayMethodSummary(unpaidAmount, dayUnpaidOrders.size());

    return new SalesSummaryRes(
        date, totalSales, prevSales, netSales, expenseTotal, cash, card, unpaid);
  }

  /* =========================================================
   * Transactions — 그날 거래 내역 (전체 응답, 클라 페이징/필터)
   * ========================================================= */

  public List<TransactionRes> transactions(TransactionsParams params) {
    OffsetDateTime[] dayRange = dayRangeOf(params.date());
    Specification<Order> spec = baseDateRange(dayRange).and(storeFilter(params.storeSeq()));
    List<Order> orders = orderRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "orderAt"));
    return assembleTransactionList(orders);
  }

  /* =========================================================
   * Unpaid — 수금 탭 (날짜 무관 전체 미수)
   * ========================================================= */

  public Page<TransactionRes> unpaid(UnpaidParams params) {
    Pageable pageable = pageableOf(params.page(), params.size());
    Specification<Order> spec =
        Specification.<Order>where((r, q, cb) -> cb.notEqual(r.get("status"), OrderStatus.PAID))
            .and(storeFilter(params.storeSeq()));
    Page<Order> orderPage = orderRepo.findAll(spec, pageable);
    List<TransactionRes> content = assembleTransactionList(orderPage.getContent());
    return new PageImpl<>(content, pageable, orderPage.getTotalElements());
  }

  /* =========================================================
   * Grid tab — orders / summary / remove
   * ========================================================= */

  /** 그리드 탭 거래 내역 — 클라 페이징 (전체 응답). UI 가드: 90일 초과 호출 금지. */
  public List<OrderRowRes> findOrders(OrdersParams params) {
    OffsetDateTime fromDt =
        params.from().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    OffsetDateTime toDt =
        params.to().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

    Specification<Order> spec =
        baseDateRange(new OffsetDateTime[] {fromDt, toDt})
            .and(storeFilter(params.storeSeq()))
            .and(menuFilter(params.menuSeq()))
            .and(payTypeFilter(params.payType()));

    List<Order> orders = orderRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "orderAt"));
    if (orders.isEmpty()) return List.of();

    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    Set<Short> storeSeqs =
        orders.stream().map(Order::getStoreSeq).collect(Collectors.toSet());

    Map<Long, List<Payment>> paymentsByOrder =
        paymentRepo.findByOrderSeqIn(orderSeqs).stream()
            .collect(Collectors.groupingBy(Payment::getOrderSeq));
    Map<Short, Store> storeMap =
        storeRepo.findAllById(storeSeqs).stream()
            .collect(Collectors.toMap(Store::getSeq, Function.identity()));
    Map<Long, List<OrderMenuExtRes>> menusByOrder =
        orderMenuRepo.findExtsByOrderSeqs(orderSeqs).stream()
            .collect(Collectors.groupingBy(OrderMenuExtRes::orderSeq));

    return orders.stream()
        .map(
            o ->
                toOrderRowRes(
                    o,
                    storeMap.get(o.getStoreSeq()),
                    paymentsByOrder.getOrDefault(o.getSeq(), List.of()),
                    menusByOrder.getOrDefault(o.getSeq(), List.of())))
        .toList();
  }

  /** 그리드 탭 KPI 4 카드. */
  public OrdersSummaryRes ordersSummary(OrdersParams params) {
    OffsetDateTime fromDt =
        params.from().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    OffsetDateTime toDt =
        params.to().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    int dayCount = (int) (params.to().toEpochDay() - params.from().toEpochDay() + 1);

    Specification<Order> spec =
        baseDateRange(new OffsetDateTime[] {fromDt, toDt})
            .and(storeFilter(params.storeSeq()))
            .and(menuFilter(params.menuSeq()))
            .and(payTypeFilter(params.payType()));

    List<Order> orders = orderRepo.findAll(spec);
    int totalSales = orders.stream().mapToInt(Order::getAmount).sum();
    int totalCount = orders.size();
    int avgDailySales = dayCount > 0 ? totalSales / dayCount : 0;
    int avgDailyCount = dayCount > 0 ? totalCount / dayCount : 0;

    // 결제 분해 — 같은 필터 기간 내 t_payment, 이 주문들에 한정
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    List<Payment> payments =
        orderSeqs.isEmpty() ? List.of() : paymentRepo.findByOrderSeqIn(orderSeqs);
    PayMethodSummary cash = aggregateBy(payments, PayType.CASH);
    PayMethodSummary card = aggregateBy(payments, PayType.CARD);

    return new OrdersSummaryRes(totalSales, totalCount, avgDailySales, avgDailyCount, cash, card);
  }

  /**
   * 다중 주문 삭제 — 회계 정정용.
   *
   * <p>{@link com.ban.cheonil.order.OrderService#remove} 와 달리 PAID 도 허용. cascade 처리:
   *
   * <ol>
   *   <li>t_payment 삭제
   *   <li>t_order_menu 삭제
   *   <li>t_order 삭제
   *   <li>SSE Removed 이벤트 발행
   * </ol>
   */
  @Transactional
  public void removeOrders(List<Long> orderSeqs) {
    for (Long seq : orderSeqs) {
      Order order =
          orderRepo
              .findById(seq)
              .orElseThrow(
                  () ->
                      new jakarta.persistence.EntityNotFoundException(
                          "order " + seq + " not found"));
      List<Payment> payments = paymentRepo.findByOrderSeq(seq);
      if (!payments.isEmpty()) paymentRepo.deleteAll(payments);
      orderMenuRepo.deleteByIdOrderSeq(seq);
      orderRepo.delete(order);
      eventPublisher.publishEvent(new OrderEvent.Removed(seq));
    }
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  private OffsetDateTime[] dayRangeOf(LocalDate date) {
    OffsetDateTime start = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    OffsetDateTime end =
        date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    return new OffsetDateTime[] {start, end};
  }

  private Pageable pageableOf(Integer page, Integer size) {
    return PageRequest.of(
        page != null ? page : 0,
        size != null ? size : 20,
        Sort.by(Sort.Direction.DESC, "orderAt"));
  }

  private PayMethodSummary aggregateBy(List<Payment> payments, PayType type) {
    int amount =
        payments.stream().filter(p -> p.getPayType() == type).mapToInt(Payment::getAmount).sum();
    int count = (int) payments.stream().filter(p -> p.getPayType() == type).count();
    return new PayMethodSummary(amount, count);
  }

  /** orders → transactions (결제 / 매장 / 메뉴 batch fetch + 조립). */
  private List<TransactionRes> assembleTransactionList(List<Order> orders) {
    if (orders.isEmpty()) return List.of();

    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    Set<Short> storeSeqs =
        orders.stream().map(Order::getStoreSeq).collect(Collectors.toSet());

    Map<Long, List<Payment>> paymentsByOrder =
        paymentRepo.findByOrderSeqIn(orderSeqs).stream()
            .collect(Collectors.groupingBy(Payment::getOrderSeq));
    Map<Short, Store> storeMap =
        storeRepo.findAllById(storeSeqs).stream()
            .collect(Collectors.toMap(Store::getSeq, Function.identity()));
    Map<Long, List<OrderMenuExtRes>> menusByOrder =
        orderMenuRepo.findExtsByOrderSeqs(orderSeqs).stream()
            .collect(Collectors.groupingBy(OrderMenuExtRes::orderSeq));

    return orders.stream()
        .map(
            o ->
                toTransactionRes(
                    o,
                    storeMap.get(o.getStoreSeq()),
                    paymentsByOrder.getOrDefault(o.getSeq(), List.of()),
                    menusByOrder.getOrDefault(o.getSeq(), List.of())))
        .toList();
  }

  /** 분할 결제 시 {@code payments} 에 다수 entry — UI 가 합계/마지막시각/분할 표기 등을 결정. */
  private TransactionRes toTransactionRes(
      Order o, Store store, List<Payment> payments, List<OrderMenuExtRes> menus) {
    return new TransactionRes(
        o.getSeq(),
        o.getStoreSeq(),
        store != null ? store.getNm() : null,
        menuSummaryOf(menus),
        o.getAmount(),
        o.getOrderAt(),
        o.getCookedAt(),
        toPaymentResList(payments));
  }

  /* ---- Specification 부품 ---- */

  private Specification<Order> baseDateRange(OffsetDateTime[] dayRange) {
    return Specification.allOf(
        (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("orderAt"), dayRange[0]),
        (r, q, cb) -> cb.lessThan(r.get("orderAt"), dayRange[1]));
  }

  private Specification<Order> storeFilter(Short storeSeq) {
    if (storeSeq == null) return (r, q, cb) -> cb.conjunction();
    return (r, q, cb) -> cb.equal(r.get("storeSeq"), storeSeq);
  }

  /** 특정 메뉴를 포함하는 주문만 — t_order_menu sub-query 로 매칭. */
  private Specification<Order> menuFilter(Short menuSeq) {
    if (menuSeq == null) return (r, q, cb) -> cb.conjunction();
    return (r, q, cb) -> {
      Subquery<Long> sub = q.subquery(Long.class);
      Root<OrderMenu> om = sub.from(OrderMenu.class);
      sub.select(om.get("id").get("orderSeq"))
          .where(cb.equal(om.get("id").get("menuSeq"), menuSeq));
      return r.get("seq").in(sub);
    };
  }

  /** {@link TransactionRes} 와 동일한 join 결과 + status / cmt 추가. */
  private OrderRowRes toOrderRowRes(
      Order o, Store store, List<Payment> payments, List<OrderMenuExtRes> menus) {
    return new OrderRowRes(
        o.getSeq(),
        o.getStoreSeq(),
        store != null ? store.getNm() : null,
        menuSummaryOf(menus),
        o.getAmount(),
        o.getOrderAt(),
        o.getCookedAt(),
        toPaymentResList(payments),
        o.getStatus(),
        o.getCmt());
  }

  private String menuSummaryOf(List<OrderMenuExtRes> menus) {
    return menus.stream()
        .map(m -> m.menuNm() + " " + m.cnt())
        .collect(Collectors.joining(", "));
  }

  private List<PaymentRes> toPaymentResList(List<Payment> payments) {
    return payments.stream()
        .map(p -> new PaymentRes(p.getPayType(), p.getAmount(), p.getPayAt()))
        .toList();
  }

  private Specification<Order> payTypeFilter(String payType) {
    if (payType == null || payType.isBlank()) return (r, q, cb) -> cb.conjunction();
    if ("UNPAID".equals(payType)) {
      return (r, q, cb) -> cb.notEqual(r.get("status"), OrderStatus.PAID);
    }
    if ("CASH".equals(payType) || "CARD".equals(payType)) {
      PayType target = PayType.valueOf(payType);
      return (r, q, cb) -> {
        Subquery<Long> sub = q.subquery(Long.class);
        Root<Payment> p = sub.from(Payment.class);
        sub.select(p.get("orderSeq")).where(cb.equal(p.get("payType"), target));
        return r.get("seq").in(sub);
      };
    }
    return (r, q, cb) -> cb.conjunction();
  }
}
