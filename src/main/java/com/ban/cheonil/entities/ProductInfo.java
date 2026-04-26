package com.ban.cheonil.entities;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_product_info")
public class ProductInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Short seq;

  @NotNull
  @Column(name = "ingd_seq", nullable = false)
  private Short ingdSeq;

  @Column(name = "brand_seq")
  private Short brandSeq;

  @Size(max = 100)
  @NotNull
  @Column(name = "nm", nullable = false, length = 100)
  private String nm;

  @Size(max = 200)
  @Column(name = "cmt", length = 200)
  private String cmt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "options")
  private Map<String, Object> options;

  @ColumnDefault("now()")
  @Column(name = "reg_at")
  private OffsetDateTime regAt;

  @ColumnDefault("now()")
  @Column(name = "mod_at")
  private OffsetDateTime modAt;
}
