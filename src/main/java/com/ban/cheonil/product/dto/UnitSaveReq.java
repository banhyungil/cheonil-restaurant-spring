package com.ban.cheonil.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * POST /units 생성 + PUT /units/{seq} 전체 교체 페이로드.
 *
 * <p>{@code baseUnitSeq} null 이면 자기 자신이 기준 단위 (baseFactor 무시). 값이 있으면 baseFactor 필수.
 */
public record UnitSaveReq(
    @NotBlank @Size(max = 40) String nm,
    @NotNull Boolean isUnitCnt,
    Short baseUnitSeq,
    /** 기준 단위 환산계수 (g → kg 이면 0.001). */
    @Positive @Digits(integer = 6, fraction = 4) BigDecimal baseFactor) {}
