package com.ban.cheonil.expense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.expense.entity.ExpenseCategory;

/** 지출 카테고리 Repository. path(ltree) 연산이 필요한 생성/이동만 native 쿼리. */
public interface ExpenseCategoryRepo extends JpaRepository<ExpenseCategory, Integer> {

  @Query(value = "select * from m_expense_category order by path", nativeQuery = true)
  List<ExpenseCategory> findAllOrderByPath();

  /** path 를 만들려면 seq 를 먼저 알아야 하므로 시퀀스에서 직접 발급. */
  @Query(
      value = "select nextval(pg_get_serial_sequence('m_expense_category', 'seq'))",
      nativeQuery = true)
  long nextSeq();

  @Modifying
  @Query(
      value =
          "insert into m_expense_category (seq, path, nm) values (:seq, cast(:path as ltree), :nm)",
      nativeQuery = true)
  void insert(@Param("seq") int seq, @Param("path") String path, @Param("nm") String nm);

  /** oldPath 하위 트리 전체를 newParentPath 아래로 이동. 노드 자신의 라벨은 유지. */
  @Modifying(clearAutomatically = true)
  @Query(
      value =
          "update m_expense_category"
              + " set path = cast(:newParentPath as ltree)"
              + "   || subpath(path, nlevel(cast(:oldPath as ltree)) - 1)"
              + " where path <@ cast(:oldPath as ltree)",
      nativeQuery = true)
  void moveUnder(@Param("oldPath") String oldPath, @Param("newParentPath") String newParentPath);

  /** oldPath 하위 트리 전체를 최상위로 이동. */
  @Modifying(clearAutomatically = true)
  @Query(
      value =
          "update m_expense_category"
              + " set path = subpath(path, nlevel(cast(:oldPath as ltree)) - 1)"
              + " where path <@ cast(:oldPath as ltree)",
      nativeQuery = true)
  void moveToRoot(@Param("oldPath") String oldPath);
}
