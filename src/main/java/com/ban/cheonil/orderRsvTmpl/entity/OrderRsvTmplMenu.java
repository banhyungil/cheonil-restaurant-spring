package com.ban.cheonil.orderRsvTmpl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_order_rsv_menu")
public class OrderRsvTmplMenu {
  @EmbeddedId private OrderRsvTmplMenuId id;

  @NotNull
  @Column(name = "price", nullable = false)
  private Integer price;

  @NotNull
  @Column(name = "cnt", nullable = false)
  private Short cnt;
}
