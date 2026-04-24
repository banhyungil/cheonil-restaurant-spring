package com.ban.cheonil.order.dto;

import java.util.List;

public record OrderCreateReq(
        Short storeSeq,
        List<OrderItemReq> items,
        String cmt
) {
}
