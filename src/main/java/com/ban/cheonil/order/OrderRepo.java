package com.ban.cheonil.order;

import com.ban.cheonil.order.dto.OrderSummary;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /** 주문 목록용 — OrderSummary projection 으로 필요한 컬럼만 조회. */
    List<OrderSummary> findAllByStatus(OrderStatus status);
}
