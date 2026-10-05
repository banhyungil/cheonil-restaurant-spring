package com.ban.cheonil.expense.entity;

import java.util.Map;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_expense_category")
public class ExpenseCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Integer seq;

  /**
   * ltree 경로 (seq 라벨). 읽기 전용 — 생성/이동은 ExpenseCategoryRepo 의 native 쿼리로만 변경한다. (JPA 가 varchar 로
   * 바인딩해 ltree 컬럼에 쓰면 타입 오류)
   */
  @NotNull
  @JdbcTypeCode(SqlTypes.OTHER)
  @Column(
      name = "path",
      nullable = false,
      columnDefinition = "ltree",
      insertable = false,
      updatable = false)
  private String path;

  @Size(max = 50)
  @NotNull
  @Column(name = "nm", nullable = false, length = 50)
  private String nm;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "options")
  private Map<String, Object> options;
}
