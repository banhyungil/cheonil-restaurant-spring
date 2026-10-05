package com.ban.cheonil.expense.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.ban.cheonil.common.config.TimeZoneConfig;
import com.ban.cheonil.expense.entity.Expense;

/**
 * 지출 응답.
 *
 * @param expenseDt 지출일자 (KST). 지출은 일자 단위로 다룬다
 * @param storeSeq 구입처 매장. 공과금 등 매장 없는 지출은 null
 * @param products 구입목록. 품목 없이 금액만 입력한 지출은 빈 배열
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
    OffsetDateTime modAt,
    List<ExpenseProductRes> products) {
  public static ExpenseRes from(Expense e, List<ExpenseProductRes> products) {
    return new ExpenseRes(
        e.getSeq(),
        e.getCtgSeq(),
        e.getStoreSeq(),
        e.getNm(),
        e.getAmount(),
        e.getExpenseAt().atZoneSameInstant(TimeZoneConfig.BUSINESS_ZONE).toLocalDate(),
        e.getCmt(),
        e.getRegAt(),
        e.getModAt(),
        products);
  }
}
