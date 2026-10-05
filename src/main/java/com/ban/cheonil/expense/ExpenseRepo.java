package com.ban.cheonil.expense;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.expense.entity.Expense;

/** 지출 Repository. */
public interface ExpenseRepo extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

  long countByCtgSeq(Integer ctgSeq);

  /** 기간 내 지출 합계. row 0건이면 0 반환. */
  @Query(
      "select coalesce(sum(e.amount), 0) from Expense e "
          + "where e.expenseAt >= :from and e.expenseAt < :to")
  Integer sumAmountByExpenseAtRange(
      @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

  /** 같은 매장 + 같은 KST 일자 지출 — uq_expense_store_day 와 같은 식이라 인덱스를 탄다. */
  @Query(
      value =
          "select * from t_expense where store_seq = :storeSeq"
              + " and (expense_at at time zone 'Asia/Seoul')::date = :date",
      nativeQuery = true)
  Optional<Expense> findByStoreAndDay(
      @Param("storeSeq") Short storeSeq, @Param("date") LocalDate date);

  /** 카테고리에서 쓴 지출명 — 최근 사용순. 지출명 추천용. */
  @Query(
      value =
          "select nm from t_expense where ctg_seq = :ctgSeq"
              + " group by nm order by max(expense_at) desc limit :limit",
      nativeQuery = true)
  List<String> findRecentNames(@Param("ctgSeq") Integer ctgSeq, @Param("limit") int limit);
}
