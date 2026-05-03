package com.ban.cheonil.orderRsvTmpl.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.ban.cheonil.orderRsvTmpl.entity.DayType;

/** 템플릿 + 매장 + 메뉴 aggregate (프론트 OrderRsvTmplExt 와 1:1 매칭). */
public record OrderRsvTmplExtRes(
    Short seq,
    Short storeSeq,
    String nm,
    Integer amount,
    LocalTime rsvTime,
    List<DayType> dayTypes,
    String cmt,
    Boolean active,
    Boolean autoOrder,
    LocalDate startDt,
    LocalDate endDt,
    OffsetDateTime regAt,
    OffsetDateTime modAt,
    String storeNm,
    List<OrderRsvTmplMenuExtRes> menus) {}
