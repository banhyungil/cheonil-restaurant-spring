package com.ban.cheonil.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.order.dto.OrderCreateReq;
import com.ban.cheonil.order.dto.OrderExtRes;
import com.ban.cheonil.order.dto.OrderMenuRes;
import com.ban.cheonil.order.dto.OrderMenuExtRes;
import com.ban.cheonil.order.dto.OrderRes;
import com.ban.cheonil.order.dto.OrderStatusChangeRes;
import com.ban.cheonil.order.dto.OrdersListParams;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderMenu;
import com.ban.cheonil.order.entity.OrderMenuId;
import com.ban.cheonil.order.entity.OrderStatus;
import com.ban.cheonil.order.sse.OrderEvent;
import com.ban.cheonil.orderRsv.entity.OrderRsv;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenu;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepo orderRepo;
  private final OrderMenuRepo orderMenuRepo;
  private final StoreRepo storeRepo;
  private final ApplicationEventPublisher eventPublisher;

  /* =========================================================
   * Create
   * ========================================================= */

  @Transactional
  public OrderRes create(OrderCreateReq ocReq) {
    int amount = ocReq.menus().stream().mapToInt(i -> i.price() * i.cnt()).sum();

    Order order = new Order();
    order.setStoreSeq(ocReq.storeSeq());
    order.setAmount(amount);
    order.setStatus(OrderStatus.READY);
    order.setCmt(ocReq.cmt());
    OffsetDateTime now = OffsetDateTime.now();
    order.setOrderAt(now);
    order.setModAt(now);
    Order saved = orderRepo.save(order);

    List<OrderMenu> menus =
        ocReq.menus().stream()
            .map(
                i -> {
                  OrderMenuId omId = new OrderMenuId();
                  omId.setMenuSeq(i.menuSeq());
                  omId.setOrderSeq(saved.getSeq());

                  OrderMenu om = new OrderMenu();
                  om.setId(omId);
                  om.setPrice(i.price());
                  om.setCnt(i.cnt());
                  return om;
                })
            .toList();

    orderMenuRepo.saveAll(menus);

    // SSE — full aggregate 로 변환해서 broadcast (REST 응답은 OrderRes 유지)
    OrderExtRes ext = assemble(List.of(saved)).getFirst();
    eventPublisher.publishEvent(new OrderEvent.Created(ext));

    return toCreateRes(saved, menus);
  }

  /**
   * 예약(t_order_rsv) → 실제 주문(t_order) 변환.
   *
   * <p>예약 상태가 RESERVED → COMPLETED 로 전이될 때 {@link
   * com.ban.cheonil.orderRsv.OrderRsvService} 가 호출. 메뉴 가격은 예약 시점 스냅샷 그대로 복사.
   *
   * @return 새로 생성된 Order
   */
  @Transactional
  public Order createFromRsv(OrderRsv rsv, List<OrderRsvMenu> rsvMenus) {
    Order order = new Order();
    order.setStoreSeq(rsv.getStoreSeq());
    order.setRsvSeq(rsv.getSeq());
    order.setAmount(rsv.getAmount());
    order.setStatus(OrderStatus.READY);
    order.setCmt(rsv.getCmt());
    OffsetDateTime now = OffsetDateTime.now();
    order.setOrderAt(now);
    order.setModAt(now);
    Order saved = orderRepo.save(order);

    List<OrderMenu> menus =
        rsvMenus.stream()
            .map(
                rm -> {
                  OrderMenuId omId = new OrderMenuId();
                  omId.setMenuSeq(rm.getId().getMenuSeq());
                  omId.setOrderSeq(saved.getSeq());

                  OrderMenu om = new OrderMenu();
                  om.setId(omId);
                  om.setPrice(rm.getPrice());
                  om.setCnt(rm.getCnt());
                  return om;
                })
            .toList();
    orderMenuRepo.saveAll(menus);

    // SSE — full aggregate broadcast (주문 모니터에 즉시 표시)
    OrderExtRes ext = assemble(List.of(saved)).getFirst();
    eventPublisher.publishEvent(new OrderEvent.Created(ext));

    return saved;
  }

  /* =========================================================
   * Read
   * ========================================================= */

  /** 필터 조건에 맞는 주문 목록 + 매장 + 메뉴 aggregate. */
  @Transactional(readOnly = true)
  public List<OrderExtRes> findByParams(OrdersListParams params) {
    List<Order> orders =
        orderRepo.findAll(buildSpec(params), Sort.by(Sort.Direction.ASC, "orderAt"));
    if (orders.isEmpty()) return List.of();
    return assemble(orders);
  }

  /** 단건 — aggregate 형태로 반환. */
  @Transactional(readOnly = true)
  public OrderExtRes findExtBySeq(Long seq) {
    Order order =
        orderRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
    return assemble(List.of(order)).getFirst();
  }

  /* =========================================================
   * Update
   * ========================================================= */

  /**
   * 상태 전이.
   *
   * <ul>
   *   <li>READY → COOKED, PAID 허용
   *   <li>COOKED → READY (조리 취소), PAID 허용
   *   <li>PAID → 전이 불가 (환불은 별도 정책)
   * </ul>
   *
   * COOKED 진입 시 cookedAt = now, READY 복귀 시 cookedAt = null.
   */
  @Transactional
  public OrderStatusChangeRes changeStatus(Long seq, OrderStatus newStatus) {
    Order order =
        orderRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
    validateTransition(order.getStatus(), newStatus);

    order.setStatus(newStatus);
    if (newStatus == OrderStatus.COOKED) order.setCookedAt(OffsetDateTime.now());
    if (newStatus == OrderStatus.READY) order.setCookedAt(null);
    order.setModAt(OffsetDateTime.now());
    // dirty checking 으로 자동 update — save() 호출 불필요

    OrderStatusChangeRes res =
        new OrderStatusChangeRes(
            order.getSeq(), order.getStatus(), order.getCookedAt(), order.getModAt());
    eventPublisher.publishEvent(new OrderEvent.StatusChanged(res));
    return res;
  }

  /**
   * 결제 취소 시 — PAID 주문을 COOKED 로 되돌림.
   *
   * <p>{@link #changeStatus} 의 일반 transition 룰은 PAID 에서의 전이를 막지만, 결제 취소는 정상 운영 절차이므로 별도 메서드로 분리.
   * 호출자는 {@code com.ban.cheonil.payment.PaymentService} 의 결제 취소 흐름 안에서만 사용해야 한다.
   */
  @Transactional
  public void revertToCookedFromPaid(Long seq) {
    Order order =
        orderRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
    if (order.getStatus() != OrderStatus.PAID) {
      throw new IllegalStateException(
          "PAID 주문만 복귀(결제 취소) 가능 (현재: " + order.getStatus() + ")");
    }
    order.setStatus(OrderStatus.COOKED);
    order.setModAt(OffsetDateTime.now());

    OrderStatusChangeRes res =
        new OrderStatusChangeRes(
            order.getSeq(), order.getStatus(), order.getCookedAt(), order.getModAt());
    eventPublisher.publishEvent(new OrderEvent.StatusChanged(res));
  }

  /**
   * 주문 전체 교체 (PUT 의미). 매장/비고/메뉴 항목을 새 값으로 바꾼다.
   *
   * <p>READY 상태에서만 허용 — 조리 시작 후엔 항목 변경 차단.
   */
  @Transactional
  public OrderExtRes update(Long seq, OrderCreateReq ocReq) {
    Order order =
        orderRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
    if (order.getStatus() != OrderStatus.READY) {
      throw new IllegalStateException(
          "READY 상태에서만 수정 가능 (현재: " + order.getStatus() + ")");
    }

    int amount = ocReq.menus().stream().mapToInt(i -> i.price() * i.cnt()).sum();
    order.setStoreSeq(ocReq.storeSeq());
    order.setAmount(amount);
    order.setCmt(ocReq.cmt());
    order.setModAt(OffsetDateTime.now());

    // 기존 메뉴 항목 전부 제거 후 재등록 (PUT — 전체 교체 의미)
    orderMenuRepo.deleteByIdOrderSeq(seq);
    List<OrderMenu> newMenus =
        ocReq.menus().stream()
            .map(
                i -> {
                  OrderMenuId omId = new OrderMenuId();
                  omId.setMenuSeq(i.menuSeq());
                  omId.setOrderSeq(seq);

                  OrderMenu om = new OrderMenu();
                  om.setId(omId);
                  om.setPrice(i.price());
                  om.setCnt(i.cnt());
                  return om;
                })
            .toList();
    orderMenuRepo.saveAll(newMenus);

    OrderExtRes ext = assemble(List.of(order)).getFirst();
    eventPublisher.publishEvent(new OrderEvent.Updated(ext));
    return ext;
  }

  /* =========================================================
   * Delete
   * ========================================================= */

  /**
   * 주문 삭제. 메뉴 항목까지 cascade 삭제.
   *
   * <p>PAID 상태는 매출 데이터 보존 위해 삭제 차단. 환불은 별도 정책으로 처리.
   */
  @Transactional
  public void remove(Long seq) {
    Order order =
        orderRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
    if (order.getStatus() == OrderStatus.PAID) {
      throw new IllegalStateException("PAID 주문은 삭제 불가 (환불 처리 필요)");
    }
    orderMenuRepo.deleteByIdOrderSeq(seq);
    orderRepo.delete(order);
    eventPublisher.publishEvent(new OrderEvent.Removed(seq));
  }

  /**
   * 예약 복구(COMPLETED → RESERVED) 시 변환됐던 주문을 삭제.
   *
   * <p>READY 상태 주문만 삭제 허용 — 조리 시작(COOKED)/결제(PAID) 된 주문은 차단.
   * 메뉴 항목도 cascade 삭제 + SSE Removed 이벤트 발행.
   */
  @Transactional
  public void removeIfReady(Long seq) {
    Order order =
        orderRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("order " + seq + " not found"));
    if (order.getStatus() != OrderStatus.READY) {
      throw new IllegalStateException(
          "READY 상태 주문만 복구(삭제) 가능 (현재: " + order.getStatus() + ")");
    }
    orderMenuRepo.deleteByIdOrderSeq(seq);
    orderRepo.delete(order);
    eventPublisher.publishEvent(new OrderEvent.Removed(seq));
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  /** Order 리스트에 매장/메뉴 정보 batch fetch + Java 측 조립. 총 3 query (orders + menus + stores). */
  private List<OrderExtRes> assemble(List<Order> orders) {
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    // Set 사용: 매장 중복 제거
    Set<Short> storeSeqs = orders.stream().map(Order::getStoreSeq).collect(Collectors.toSet());

    // orderSeq로 grouping. order 조립시에 바로 꺼내기 위함
    Map<Long, List<OrderMenuExtRes>> menusByOrder =
        orderMenuRepo.findExtsByOrderSeqs(orderSeqs).stream()
            .collect(Collectors.groupingBy(OrderMenuExtRes::orderSeq));

    // toMap 사용: list -> map으로 변환
    Map<Short, Store> storeMap =
        storeRepo.findAllById(storeSeqs).stream()
            .collect(Collectors.toMap(Store::getSeq, Function.identity()));

    return orders.stream()
        .map(
            o ->
                toExtRes(
                    o,
                    storeMap.get(o.getStoreSeq()),
                    menusByOrder.getOrDefault(o.getSeq(), List.of())))
        .toList();
  }

  private OrderExtRes toExtRes(Order o, Store store, List<OrderMenuExtRes> menus) {
    return new OrderExtRes(
        o.getSeq(),
        o.getStoreSeq(),
        o.getRsvSeq(),
        o.getAmount(),
        o.getStatus(),
        o.getOrderAt(),
        o.getCookedAt(),
        o.getCmt(),
        o.getModAt(),
        store != null ? store.getNm() : null,
        store != null ? store.getCmt() : null,
        menus);
  }

  // Specification — Spring Data JPA 가 Criteria API 를 감싼 DSL ⭐
  private Specification<Order> buildSpec(OrdersListParams p) {
    // 함수형 인터페이스에 람다를 할당, 즉 인터페이스에 구현체를 할당
    // Java 8+ 의 functional interface
    Specification<Order> spec = (r, q, cb) -> cb.conjunction();

    if (p.statuses() != null && !p.statuses().isEmpty()) {
      spec = spec.and((r, q, cb) -> r.get("status").in(p.statuses()));
    }
    if (p.cookedSince() != null) {
      // COOKED 만 cookedAt 하한 적용. 다른 status 는 통과.
      spec =
          spec.and(
              (r, q, cb) ->
                  cb.or(
                      cb.notEqual(r.get("status"), OrderStatus.COOKED),
                      cb.greaterThanOrEqualTo(r.get("cookedAt"), p.cookedSince())));
    }
    if (p.storeSeq() != null) {
      spec = spec.and((r, q, cb) -> cb.equal(r.get("storeSeq"), p.storeSeq()));
    }
    if (p.orderFrom() != null) {
      spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("orderAt"), p.orderFrom()));
    }
    if (p.orderTo() != null) {
      spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("orderAt"), p.orderTo()));
    }
    return spec;
  }

  private void validateTransition(OrderStatus from, OrderStatus to) {
    boolean ok =
        switch (from) {
          case READY -> to == OrderStatus.COOKED || to == OrderStatus.PAID;
          case COOKED -> to == OrderStatus.READY || to == OrderStatus.PAID;
          case PAID -> false;
        };
    if (!ok) {
      throw new IllegalStateException("invalid transition: " + from + " -> " + to);
    }
  }

  private OrderRes toCreateRes(Order order, List<OrderMenu> menus) {
    List<OrderMenuRes> menuItems =
        menus.stream()
            .map(m -> new OrderMenuRes(m.getId().getMenuSeq(), m.getPrice(), m.getCnt()))
            .toList();
    return new OrderRes(
        order.getSeq(),
        order.getStoreSeq(),
        order.getAmount(),
        order.getStatus(),
        order.getOrderAt(),
        order.getCookedAt(),
        order.getCmt(),
        menuItems);
  }
}
