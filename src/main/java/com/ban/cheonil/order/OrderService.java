package com.ban.cheonil.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.order.dto.OrderCreateReq;
import com.ban.cheonil.order.dto.OrderExtRes;
import com.ban.cheonil.order.dto.OrderItemRes;
import com.ban.cheonil.order.dto.OrderMenuExtRes;
import com.ban.cheonil.order.dto.OrderRes;
import com.ban.cheonil.order.dto.OrdersListParams;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderMenu;
import com.ban.cheonil.order.entity.OrderMenuId;
import com.ban.cheonil.order.entity.OrderStatus;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepo orderRepo;
  private final OrderMenuRepo orderMenuRepo;
  private final StoreRepo storeRepo;

  /* =========================================================
   * Create
   * ========================================================= */

  @Transactional
  public OrderRes create(OrderCreateReq ocReq) {
    int amount = ocReq.items().stream().mapToInt(i -> i.price() * i.cnt()).sum();

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
        ocReq.items().stream()
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

    return toCreateRes(saved, menus);
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
  public OrderExtRes changeStatus(Long seq, OrderStatus newStatus) {
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

    return assemble(List.of(order)).getFirst();
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
    List<OrderItemRes> items =
        menus.stream()
            .map(m -> new OrderItemRes(m.getId().getMenuSeq(), m.getPrice(), m.getCnt()))
            .toList();
    return new OrderRes(
        order.getSeq(),
        order.getStoreSeq(),
        order.getAmount(),
        order.getStatus(),
        order.getOrderAt(),
        order.getCookedAt(),
        order.getCmt(),
        items);
  }
}
