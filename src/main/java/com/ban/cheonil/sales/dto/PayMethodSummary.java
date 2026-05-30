package com.ban.cheonil.sales.dto;

/** 결제 수단별 합계 / 건수. amount=공급가(net), vat=부가세. 실수령액=amount+vat (표시용 합산은 프론트). */
public record PayMethodSummary(Integer amount, Integer vat, Integer count) {
  public static PayMethodSummary empty() {
    return new PayMethodSummary(0, 0, 0);
  }
}
