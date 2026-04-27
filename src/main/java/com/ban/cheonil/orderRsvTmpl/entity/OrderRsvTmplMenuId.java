package com.ban.cheonil.orderRsvTmpl.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 템플릿-메뉴 복합키. (menu_seq, rsv_tmpl_seq) 조합. */
@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class OrderRsvTmplMenuId implements Serializable {
  private static final long serialVersionUID = -3170649659091315288L;

  @NotNull
  @Column(name = "menu_seq", nullable = false)
  private Short menuSeq;

  @NotNull
  @Column(name = "rsv_tmpl_seq", nullable = false)
  private Short rsvTmplSeq;
}
