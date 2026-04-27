package com.ban.cheonil.orderRsvTmpl.dto;

/** 템플릿 메뉴 + 메뉴 정보 join (프론트 OrderRsvTmplMenuExt 와 1:1). */
public record OrderRsvTmplMenuExtRes(
    Short menuSeq, Short rsvTmplSeq, Integer price, Short cnt, String menuNm, String menuNmS) {}
