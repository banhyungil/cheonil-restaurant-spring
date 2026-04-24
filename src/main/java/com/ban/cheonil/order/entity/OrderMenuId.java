package com.ban.cheonil.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class OrderMenuId implements Serializable {
    private static final long serialVersionUID = 921846760410308980L;

    @NotNull
    @Column(name = "menu_seq", nullable = false)
    private Short menuSeq;

    @NotNull
    @Column(name = "order_seq", nullable = false)
    private Long orderSeq;
}
