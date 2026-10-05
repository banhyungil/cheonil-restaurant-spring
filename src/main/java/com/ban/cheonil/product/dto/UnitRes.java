package com.ban.cheonil.product.dto;

import java.math.BigDecimal;

import com.ban.cheonil.product.entity.Unit;

public record UnitRes(
    Short seq, String nm, Boolean isUnitCnt, Short baseUnitSeq, BigDecimal baseFactor) {
  public static UnitRes from(Unit u) {
    return new UnitRes(
        u.getSeq(), u.getNm(), u.getIsUnitCnt(), u.getBaseUnitSeq(), u.getBaseFactor());
  }
}
