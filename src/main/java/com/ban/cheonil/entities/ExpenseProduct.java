package com.ban.cheonil.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_expense_product")
public class ExpenseProduct {
  @EmbeddedId private ExpenseProductId id;

  @NotNull
  @Column(name = "cnt", nullable = false)
  private Short cnt;

  @NotNull
  @Column(name = "price", nullable = false)
  private Long price;

  @Column(name = "unit_cnt")
  private Short unitCnt;

  @Size(max = 400)
  @Column(name = "cmt", length = 400)
  private String cmt;
}
