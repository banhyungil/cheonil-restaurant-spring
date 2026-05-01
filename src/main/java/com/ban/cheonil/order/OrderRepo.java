package com.ban.cheonil.order;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.order.dto.OrderSummary;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderStatus;

public interface OrderRepo extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

  /** 주문 목록용 — OrderSummary projection 으로 필요한 컬럼만 조회. */
  List<OrderSummary> findAllByStatus(OrderStatus status);

  /** 멱등성 체크 — 특정 예약(rsvSeq) 으로 이미 t_order 가 생성됐는지. */
  boolean existsByRsvSeq(Long rsvSeq);

  /** 정산 KPI — 기간 내 주문 amount 합계. row 0건이면 null 반환 (서비스 단에서 0 처리). */
  @Query(
      "select coalesce(sum(o.amount), 0) from Order o "
          + "where o.orderAt >= :from and o.orderAt < :to")
  Integer sumAmountByOrderAtRange(
      @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

  /** 미수 주문 — status != PAID. 정산 KPI 의 unpaid 카드용 (날짜 무관 전체 미수). */
  List<Order> findByStatusNot(OrderStatus status);
}
