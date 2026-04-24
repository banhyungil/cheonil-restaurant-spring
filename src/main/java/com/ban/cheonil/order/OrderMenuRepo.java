package com.ban.cheonil.order;

import com.ban.cheonil.order.entity.OrderMenu;
import com.ban.cheonil.order.entity.OrderMenuId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderMenuRepo extends JpaRepository<OrderMenu, OrderMenuId> {
}
