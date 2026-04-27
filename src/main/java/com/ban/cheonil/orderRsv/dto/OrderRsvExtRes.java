package com.ban.cheonil.orderRsv.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.ban.cheonil.orderRsv.entity.RsvStatus;

/**
 * 예약 + 매장 + 템플릿 + 메뉴 aggregate (프론트 OrderRsvExt 와 1:1 매칭).
 *
 * <p>{@code cmt} 는 t_order_rsv 컬럼 미존재 → 항상 null. 추후 컬럼 추가 시 보강.
 */
public record OrderRsvExtRes(
    Long seq,
    Short storeSeq,
    Short rsvTmplSeq,
    Integer amount,
    OffsetDateTime rsvAt,
    RsvStatus status,
    String cmt,
    OffsetDateTime regAt,
    OffsetDateTime modAt,
    // m_store join 으로 채워지는 필드
    String storeNm,
    String storeCmt,
    // m_order_rsv_tmpl join 으로 채워지는 필드 (템플릿 유래일 때만)
    String tmplNm,
    // t_order_rsv_menu + m_menu join
    List<OrderRsvMenuExtRes> menus) {}
