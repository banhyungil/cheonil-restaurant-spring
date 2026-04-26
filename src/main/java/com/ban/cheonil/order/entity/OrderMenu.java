package com.ban.cheonil.order.entity;

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
@Table(name = "t_order_menu")
public class OrderMenu {
  @EmbeddedId private OrderMenuId id;

  @NotNull
  @Column(name = "price", nullable = false)
  private Integer price;

  @NotNull
  @Column(name = "cnt", nullable = false)
  private Short cnt;
}
