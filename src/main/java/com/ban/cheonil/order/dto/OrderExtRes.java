package com.ban.cheonil.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.ban.cheonil.order.entity.OrderStatus;

/** 주문 + 매장 + 메뉴 aggregate (프론트 OrderExt 와 1:1 매칭). 주문현황 모니터 / 주문 목록 / 단건 상세 응답에 공용 사용. */
public record OrderExtRes(
    Long seq,
    Short storeSeq,
    Long rsvSeq,
    Integer amount,
    OrderStatus status,
    OffsetDateTime orderAt,
    OffsetDateTime cookedAt,
    String cmt,
    OffsetDateTime modAt,
    // m_store join 으로 채워지는 필드
    String storeNm,
    String storeCmt,
    // t_order_menu + m_menu join
    List<OrderMenuExtRes> menus) {}
