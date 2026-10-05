package com.ban.cheonil.expense;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.expense.entity.Expense;

/** 지출 Repository. */
public interface ExpenseRepo extends JpaRepository<Expense, Long> {

  long countByCtgSeq(Integer ctgSeq);

  /** 기간 내 지출 합계. row 0건이면 0 반환. */
  @Query(
      "select coalesce(sum(e.amount), 0) from Expense e "
          + "where e.expenseAt >= :from and e.expenseAt < :to")
  Integer sumAmountByExpenseAtRange(
      @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
