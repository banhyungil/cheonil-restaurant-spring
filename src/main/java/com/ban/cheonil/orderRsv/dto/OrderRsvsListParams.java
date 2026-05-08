package com.ban.cheonil.orderRsv.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.ban.cheonil.orderRsv.entity.RsvStatus;

/**
 * GET /order-rsvs query 파라미터. 모든 필드 optional.
 *
 * <p>{@code fromDate} / {@code toDate} — 예약일(rsvAt) 기간 필터, yyyy-MM-dd, 두 값 모두 inclusive.
 * 둘 다 null 이면 기간 무제한 (전체 조회). 당일만 필요하면 두 값 모두 today 로 전달.
 */
public record OrderRsvsListParams(
    List<RsvStatus> statuses,
    Short storeSeq,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {}
