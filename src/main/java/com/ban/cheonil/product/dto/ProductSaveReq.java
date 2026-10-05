package com.ban.cheonil.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * POST /products 생성 + PUT /products/{seq} 전체 교체 페이로드.
 *
 * <p>식자재는 이름으로 찾고 없으면 생성, 제품정보는 (식자재, 제품명) 으로 찾고 없으면 생성한다.
 *
 * @param unitCnts 규격 목록. 단위가 단위수량 미사용이면 무시(빈 배열로 저장)
 */
public record ProductSaveReq(
    @NotBlank @Size(max = 100) String ingdNm,
    @NotBlank @Size(max = 100) String nm,
    @NotNull Short unitSeq,
    List<@NotNull @Positive @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2) BigDecimal>
        unitCnts) {}
