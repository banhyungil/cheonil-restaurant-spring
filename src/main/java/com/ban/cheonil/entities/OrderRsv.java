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
@Table(name = "t_order_rsv")
public class OrderRsv {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long seq;

  @NotNull
  @Column(name = "store_seq", nullable = false)
  private Short storeSeq;

  @Column(name = "rsv_tmpl_seq")
  private Short rsvTmplSeq;

  @NotNull
  @Column(name = "amount", nullable = false)
  private Integer amount;

  @NotNull
  @Column(name = "rsv_at", nullable = false)
  private OffsetDateTime rsvAt;

  @ColumnDefault("'RESERVED'")
  @Column(name = "status", columnDefinition = "rsv_status not null")
  private Object status;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "reg_at", nullable = false)
  private OffsetDateTime regAt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "mod_at", nullable = false)
  private OffsetDateTime modAt;
}
