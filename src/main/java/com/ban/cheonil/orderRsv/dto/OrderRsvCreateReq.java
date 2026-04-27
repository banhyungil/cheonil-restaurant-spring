package com.ban.cheonil.orderRsv.dto;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * 예약 생성/수정 (PUT 교체) 공통 페이로드.
 *
 * <p>{@code cmt} 는 현재 DB 에 컬럼 미존재 — 받지만 무시. 추후 컬럼 추가 시 사용.
 */
public record OrderRsvCreateReq(
    Short storeSeq,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime rsvAt,
    String cmt,
    List<OrderRsvMenuReq> menus) {}
