package com.ban.cheonil.entities;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Expense 도메인 미정 단계 — 정산(Sales) 집계용 sum 쿼리만 임시로 둠. 추후 expense 도메인 패키지 분리 시 함께 이동.
 */
public interface ExpenseRepo extends JpaRepository<Expense, Long> {

  /** 기간 내 지출 합계. row 0건이면 0 반환. */
  @Query(
      "select coalesce(sum(e.amount), 0) from Expense e "
          + "where e.expenseAt >= :from and e.expenseAt < :to")
  Integer sumAmountByExpenseAtRange(
      @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
