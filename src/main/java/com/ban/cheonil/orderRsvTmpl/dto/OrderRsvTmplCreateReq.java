package com.ban.cheonil.orderRsvTmpl.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.ban.cheonil.orderRsvTmpl.entity.DayType;

/** 템플릿 생성/수정 (PUT 교체) 공통 페이로드. */
public record OrderRsvTmplCreateReq(
    Short storeSeq,
    String nm,
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime rsvTime,
    List<DayType> dayTypes,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDt,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDt,
    String cmt,
    Boolean active,
    /** true 면 스케줄러가 예약 생성 시 주문(t_order)도 즉시 생성. null = false 처리. */
    Boolean autoOrder,
    List<OrderRsvTmplMenuReq> menus) {}
