package com.ban.cheonil.order.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.order.entity.OrderStatus;

/**
 * 주문 목록 화면용 요약 projection. 엔티티 전체가 아닌 필요한 컬럼만 조회 → payload 감소 + 쿼리 최적화. Spring Data JPA 가 런타임에 프록시
 * 구현체를 만들어준다. getter 이름은 Order 엔티티의 필드명과 정확히 매칭되어야 한다.
 */
public interface OrderSummary {
  Long getSeq();

  Short getStoreSeq();

  Integer getAmount();

  OrderStatus getStatus();

  OffsetDateTime getOrderAt();
}
