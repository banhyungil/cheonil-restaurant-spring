package com.ban.cheonil.orderRsv.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 예약-메뉴 복합키. (menu_seq, rsv_seq) 조합. */
@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class OrderRsvMenuId implements Serializable {
  private static final long serialVersionUID = -7604883629102087672L;

  @NotNull
  @Column(name = "menu_seq", nullable = false)
  private Short menuSeq;

  @NotNull
  @Column(name = "rsv_seq", nullable = false)
  private Long rsvSeq;
}
