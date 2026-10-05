package com.ban.cheonil.expense.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.ban.cheonil.common.config.TimeZoneConfig;
import com.ban.cheonil.expense.entity.Expense;

/**
 * 지출 응답.
 *
 * @param expenseDt 지출일자 (KST). 지출은 일자 단위로 다룬다
 * @param storeSeq 구입처 매장. 공과금 등 매장 없는 지출은 null
 */
public record ExpenseRes(
    Long seq,
    Integer ctgSeq,
    Short storeSeq,
    String nm,
    Integer amount,
    LocalDate expenseDt,
    String cmt,
    OffsetDateTime regAt,
    OffsetDateTime modAt) {
  public static ExpenseRes from(Expense e) {
    return new ExpenseRes(
        e.getSeq(),
        e.getCtgSeq(),
        e.getStoreSeq(),
        e.getNm(),
        e.getAmount(),
        e.getExpenseAt().atZoneSameInstant(TimeZoneConfig.BUSINESS_ZONE).toLocalDate(),
        e.getCmt(),
        e.getRegAt(),
        e.getModAt());
  }
}
