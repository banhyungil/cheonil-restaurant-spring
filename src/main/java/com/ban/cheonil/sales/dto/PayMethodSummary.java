package com.ban.cheonil.sales.dto;

/** 결제 수단별 합계 / 건수. */
public record PayMethodSummary(Integer amount, Integer count) {
  public static PayMethodSummary empty() {
    return new PayMethodSummary(0, 0);
  }
}
