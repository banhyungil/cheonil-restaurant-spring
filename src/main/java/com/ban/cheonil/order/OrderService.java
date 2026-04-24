package com.ban.cheonil.order;

import com.ban.cheonil.order.dto.OrderCreateReq;
import com.ban.cheonil.order.dto.OrderItemRes;
import com.ban.cheonil.order.dto.OrderRes;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderMenu;
import com.ban.cheonil.order.entity.OrderMenuId;
import com.ban.cheonil.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepo orderRepo;
    private final OrderMenuRepo orderMenuRepo;

    @Transactional
    public OrderRes create(OrderCreateReq req) {
        int amount = req.items().stream()
                .mapToInt(i -> i.price() * i.cnt())
                .sum();

        Order order = new Order();
        order.setStoreSeq(req.storeSeq());
        order.setAmount(amount);
        order.setStatus(OrderStatus.READY);
        order.setCmt(req.cmt());
        OffsetDateTime now = OffsetDateTime.now();
        order.setOrderAt(now);
        order.setModAt(now);
        Order saved = orderRepo.save(order);

        List<OrderMenu> menus = req.items().stream().map(i -> {
            OrderMenuId id = new OrderMenuId();
            id.setMenuSeq(i.menuSeq());
            id.setOrderSeq(saved.getSeq());

            OrderMenu om = new OrderMenu();
            om.setId(id);
            om.setPrice(i.price());
            om.setCnt(i.cnt());
            return om;
        }).toList();
        orderMenuRepo.saveAll(menus);

        return toRes(saved, menus);
    }

    private OrderRes toRes(Order order, List<OrderMenu> menus) {
        List<OrderItemRes> items = menus.stream()
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
                items
        );
    }
}
