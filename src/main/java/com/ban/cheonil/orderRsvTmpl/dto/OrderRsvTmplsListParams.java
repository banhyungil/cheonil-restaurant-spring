package com.ban.cheonil.orderRsvTmpl.dto;

import com.ban.cheonil.orderRsvTmpl.entity.DayType;

/**
 * GET /order-rsv-tmpls query 파라미터. 모든 필드 optional.
 *
 * <p>{@code dayType} 필터링은 day_types 배열에 해당 요일이 포함된 템플릿만 (PG ANY 연산자).
 */
public record OrderRsvTmplsListParams(Short storeSeq, DayType dayType, Boolean active) {}
