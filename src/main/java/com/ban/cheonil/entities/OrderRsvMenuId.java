package com.ban.cheonil.entities;

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
public class OrderRsvMenuId implements Serializable {
    private static final long serialVersionUID = -7604883629102087672L;
    @NotNull
    @Column(name = "menu_seq", nullable = false)
    private Short menuSeq;

    @NotNull
    @Column(name = "rsv_seq", nullable = false)
    private Long rsvSeq;


}