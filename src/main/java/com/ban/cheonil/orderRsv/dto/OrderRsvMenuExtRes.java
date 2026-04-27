package com.ban.cheonil.orderRsv.dto;

/** 예약 메뉴 + 메뉴 정보 join (프론트 OrderRsvMenuExt 와 1:1 매칭). */
public record OrderRsvMenuExtRes(
    Short menuSeq,
    Long rsvSeq,
    Integer price,
    Short cnt,
    // m_menu join 으로 채워지는 필드
    String menuNm,
    String menuNmS) {}
