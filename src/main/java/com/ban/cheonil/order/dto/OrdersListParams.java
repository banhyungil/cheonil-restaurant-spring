package com.ban.cheonil.order.dto;

import com.ban.cheonil.order.entity.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /orders 의 query parameter 묶음.
 * 모든 필드 optional — null 인 항목은 필터에서 제외된다.
 * <p>
 * Spring 6+ 의 record canonical constructor binding 을 사용. 컨트롤러에서
 * {@code @ModelAttribute OrdersListParams params} 로 받음.
 */
public record OrdersListParams(
        /** 조회 상태들. 미지정 시 전체. */
        List<OrderStatus> statuses,
        /** COOKED 주문 cookedAt 하한 (ISO datetime). 모니터 윈도우 제한용. */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime cookedSince,
        /** 매장 필터. */
        Short storeSeq,
        /** 주문 시각 범위 시작 (ISO datetime). 날짜별 조회용. */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime orderFrom,
        /** 주문 시각 범위 종료 (ISO datetime). 날짜별 조회용. */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime orderTo
) {
}
