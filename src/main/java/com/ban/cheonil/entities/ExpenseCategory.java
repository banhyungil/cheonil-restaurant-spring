package com.ban.cheonil.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "m_expense_category")
public class ExpenseCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer seq;

    @Column(name = "path", columnDefinition = "ltree not null")
    private Object path;

    @Size(max = 50)
    @NotNull
    @Column(name = "nm", nullable = false, length = 50)
    private String nm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private Map<String, Object> options;


}