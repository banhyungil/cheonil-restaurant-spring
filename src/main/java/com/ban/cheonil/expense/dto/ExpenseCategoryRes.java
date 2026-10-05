package com.ban.cheonil.expense.dto;

import com.ban.cheonil.expense.entity.ExpenseCategory;

/**
 * 지출 카테고리 응답. flat 리스트로 내려주고 프론트에서 트리로 조립한다.
 *
 * @param path ltree 경로 (seq 라벨, 예: "1.5.12")
 * @param parentSeq 상위 카테고리. 최상위면 null
 */
public record ExpenseCategoryRes(Integer seq, String path, Integer parentSeq, String nm) {
  public static ExpenseCategoryRes from(ExpenseCategory c) {
    String path = c.getPath();
    int idx = path.lastIndexOf('.');
    Integer parentSeq =
        idx < 0 ? null : Integer.valueOf(path.substring(path.lastIndexOf('.', idx - 1) + 1, idx));
    return new ExpenseCategoryRes(c.getSeq(), path, parentSeq, c.getNm());
  }
}
