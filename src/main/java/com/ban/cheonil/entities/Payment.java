package com.ban.cheonil.entities;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;

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

  @ColumnDefault("'CASH'")
  @Column(name = "pay_type", columnDefinition = "pay_type not null")
  private Object payType;

  @NotNull
  @Column(name = "pay_at", nullable = false)
  private OffsetDateTime payAt;
}
