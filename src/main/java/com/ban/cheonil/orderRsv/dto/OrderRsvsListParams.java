package com.ban.cheonil.orderRsv.dto;

import java.util.List;

import com.ban.cheonil.orderRsv.entity.RsvStatus;

/**
 * GET /order-rsvs query 파라미터. 모든 필드 optional.
 *
 * <p>{@code dayMode} 는 메인 페이지 세그먼트용 — TODAY 면 오늘 0시~24시 범위, ALL 또는 null 이면 전체.
 */
public record OrderRsvsListParams(
    List<RsvStatus> statuses, DayMode dayMode, Short storeSeq) {

  public enum DayMode {
    TODAY,
    ALL
  }
}
