package com.ban.cheonil.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /menus 생성 + PUT /menus/{seq} 전체 교체 페이로드.
 *
 * <p>{@code active} null 이면 default true.
 */
public record MenuCreateReq(
    @NotNull Short ctgSeq,
    @NotBlank @Size(max = 45) String nm,
    /** 메뉴명 축약 — 좁은 영역 표시용 (최대 4자 권장, DB 제약은 10자). */
    @Size(max = 10) String nmS,
    @NotNull @PositiveOrZero Integer price,
    @Size(max = 1000) String cmt,
    Boolean active) {}
