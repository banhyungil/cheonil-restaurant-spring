package com.ban.cheonil.product.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_unit")
public class Unit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Short seq;

  @Size(max = 40)
  @NotNull
  @Column(name = "nm", nullable = false, length = 40)
  private String nm;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_unit_cnt", nullable = false)
  private Boolean isUnitCnt;

  /** 기준 단위 (g → kg). NULL 이면 환산 불가. */
  @Column(name = "base_unit_seq")
  private Short baseUnitSeq;

  /** 기준 단위 환산계수 (g → 0.001). 기준단가 = price / (unit_cnt * base_factor). */
  @Column(name = "base_factor", precision = 10, scale = 4)
  private BigDecimal baseFactor;
}
