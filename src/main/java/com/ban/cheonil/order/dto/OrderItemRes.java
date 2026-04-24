package com.ban.cheonil.order.dto;

public record OrderItemRes(
        Short menuSeq,
        Integer price,
        Short cnt
) {
}
