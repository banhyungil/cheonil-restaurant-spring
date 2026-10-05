package com.ban.cheonil.expense.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 지출 품목 (구입목록 한 줄).
 *
 * @param price 단가 (줄 합계 = cnt * price)
 * @param unitCnt 구입한 규격 (예: 600g 의 600). 단위수량 미사용 단위의 제품이면 무시
 */
public record ExpenseProductReq(
    @NotNull Integer prdSeq,
    @NotNull @Positive @Max(Short.MAX_VALUE) Integer cnt,
    @NotNull @PositiveOrZero Integer price,
    @Positive @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2) BigDecimal unitCnt,
    @Size(max = 400) String cmt) {}
