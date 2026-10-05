package com.ban.cheonil.expense.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/** 지출 제품 상세. 품목 식별 = (지출, 제품, 단위수량). */
@Getter
@Setter
@Entity
@Table(name = "t_expense_product")
public class ExpenseProduct {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long seq;

  @NotNull
  @Column(name = "exps_seq", nullable = false)
  private Long expsSeq;

  @NotNull
  @Column(name = "prd_seq", nullable = false)
  private Integer prdSeq;

  @NotNull
  @Column(name = "cnt", nullable = false)
  private Short cnt;

  /** 단가 (줄 합계 = cnt * price). */
  @NotNull
  @Column(name = "price", nullable = false)
  private Integer price;

  /** 구입 시 선택한 단위수량 (m_product.unit_cnts 중 하나 또는 직접 입력). */
  @Column(name = "unit_cnt", precision = 6, scale = 2)
  private BigDecimal unitCnt;

  @Size(max = 400)
  @Column(name = "cmt", length = 400)
  private String cmt;
}
