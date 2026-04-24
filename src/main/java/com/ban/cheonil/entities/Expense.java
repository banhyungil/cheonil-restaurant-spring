package com.ban.cheonil.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "t_expense")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long seq;

    @NotNull
    @Column(name = "ctg_seq", nullable = false)
    private Integer ctgSeq;

    @Column(name = "store_seq")
    private Short storeSeq;

    @Size(max = 50)
    @NotNull
    @Column(name = "nm", nullable = false, length = 50)
    private String nm;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @NotNull
    @Column(name = "expense_at", nullable = false)
    private OffsetDateTime expenseAt;

    @Size(max = 400)
    @Column(name = "cmt", length = 400)
    private String cmt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private Map<String, Object> options;

    @ColumnDefault("now()")
    @Column(name = "mod_at")
    private OffsetDateTime modAt;


}