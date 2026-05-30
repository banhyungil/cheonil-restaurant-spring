package com.ban.cheonil.payment.entity;

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
@Table(name = "t_payment")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long seq;

  @NotNull
  @Column(name = "order_seq", nullable = false)
  private Long orderSeq;

  @NotNull
  @Column(name = "amount", nullable = false)
  private Integer amount;

  /** 부가세 — 공급가 {@link #amount} 와 분리 보관. 실수령액 = amount + vat. CARD 만 부과, CASH 는 0. */
  @NotNull
  @ColumnDefault("0")
  @Column(name = "vat", nullable = false)
  private Integer vat;

  @NotNull
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @ColumnDefault("'CASH'")
  @Column(name = "pay_type", nullable = false, columnDefinition = "pay_type")
  private PayType payType;

  @NotNull
  @Column(name = "pay_at", nullable = false)
  private OffsetDateTime payAt;
}
