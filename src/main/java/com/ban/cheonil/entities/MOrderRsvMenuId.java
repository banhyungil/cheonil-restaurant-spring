package com.ban.cheonil.entities;

// t_order_rsv 존재로 M prefix를 유지

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class MOrderRsvMenuId implements Serializable {
  private static final long serialVersionUID = -3170649659091315288L;

  @NotNull
  @Column(name = "menu_seq", nullable = false)
  private Short menuSeq;

  @NotNull
  @Column(name = "rsv_tmpl_seq", nullable = false)
  private Short rsvTmplSeq;
}
