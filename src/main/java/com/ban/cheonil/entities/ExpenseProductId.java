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
public class ExpenseProductId implements Serializable {
    private static final long serialVersionUID = 9145809788978464802L;
    @NotNull
    @Column(name = "exps_seq", nullable = false)
    private Long expsSeq;

    @NotNull
    @Column(name = "prd_seq", nullable = false)
    private Integer prdSeq;


}