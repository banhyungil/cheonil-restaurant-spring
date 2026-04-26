package com.ban.cheonil.order.entity;

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
@Table(name = "t_order")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long seq;

  @NotNull
  @Column(name = "store_seq", nullable = false)
  private Short storeSeq;

  @Column(name = "rsv_seq")
  private Long rsvSeq;

  @NotNull
  @Column(name = "amount", nullable = false)
  private Integer amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @ColumnDefault("'READY'")
  @Column(name = "status", nullable = false, columnDefinition = "order_status")
  private OrderStatus status;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "order_at", nullable = false)
  private OffsetDateTime orderAt;

  @Column(name = "cooked_at")
  private OffsetDateTime cookedAt;

  @Column(name = "cmt", length = 1000)
  private String cmt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "mod_at", nullable = false)
  private OffsetDateTime modAt;
}
