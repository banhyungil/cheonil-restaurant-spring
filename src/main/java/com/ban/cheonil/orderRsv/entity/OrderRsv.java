package com.ban.cheonil.orderRsv.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_order_rsv")
public class OrderRsv {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long seq;

  @NotNull
  @Column(name = "store_seq", nullable = false)
  private Short storeSeq;

  /** 템플릿 유래일 때만 값 존재. null = 일회성. */
  @Column(name = "rsv_tmpl_seq")
  private Short rsvTmplSeq;

  @NotNull
  @Column(name = "amount", nullable = false)
  private Integer amount;

  @NotNull
  @Column(name = "rsv_at", nullable = false)
  private OffsetDateTime rsvAt;

  @NotNull
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @ColumnDefault("'RESERVED'")
  @Column(name = "status", nullable = false, columnDefinition = "rsv_status")
  private RsvStatus status;

  @Column(name = "cmt", length = 1000)
  private String cmt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "reg_at", nullable = false)
  private OffsetDateTime regAt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "mod_at", nullable = false)
  private OffsetDateTime modAt;
}
