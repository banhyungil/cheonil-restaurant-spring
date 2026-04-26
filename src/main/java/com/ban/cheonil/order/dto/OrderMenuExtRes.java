package com.ban.cheonil.order.dto;

/** 주문 메뉴 + 메뉴 정보 join (프론트 OrderMenuExt 와 1:1 매칭). */
public record OrderMenuExtRes(
    Short menuSeq,
    Long orderSeq,
    Integer price,
    Short cnt,
    // m_menu join 으로 채워지는 필드
    String menuNm,
    String menuNmS) {}
