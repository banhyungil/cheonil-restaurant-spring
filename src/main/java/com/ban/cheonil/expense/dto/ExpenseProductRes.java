package com.ban.cheonil.expense.dto;

import java.math.BigDecimal;

import com.ban.cheonil.expense.entity.ExpenseProduct;

/**
 * 지출 품목 응답. 제품명 / 단위명은 프론트가 제품 목록(캐시)으로 매핑한다.
 *
 * @param price 단가 (줄 합계 = cnt * price)
 */
public record ExpenseProductRes(
    Long seq, Integer prdSeq, Short cnt, Integer price, BigDecimal unitCnt, String cmt) {
  public static ExpenseProductRes from(ExpenseProduct p) {
    return new ExpenseProductRes(
        p.getSeq(), p.getPrdSeq(), p.getCnt(), p.getPrice(), p.getUnitCnt(), p.getCmt());
  }
}
