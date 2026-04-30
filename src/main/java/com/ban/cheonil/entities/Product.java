package com.ban.cheonil.entities;

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
@Table(name = "m_product")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Integer seq;

  @NotNull
  @Column(name = "prd_info_seq", nullable = false)
  private Short prdInfoSeq;

  @NotNull
  @Column(name = "unit_seq", nullable = false)
  private Short unitSeq;

  @Size(max = 1000)
  @Column(name = "cmt", length = 1000)
  private String cmt;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "unit_cnts", columnDefinition = "numeric(6, 2)[]")
  private java.math.BigDecimal[] unitCnts;
}
