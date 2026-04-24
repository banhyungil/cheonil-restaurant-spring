package com.ban.cheonil.menu.entity;

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
@Table(name = "m_menu_category")
public class MenuCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Short seq;

    @Size(max = 20)
    @NotNull
    @Column(name = "nm", nullable = false, length = 20)
    private String nm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private Map<String, Object> options;

    @ColumnDefault("now()")
    @Column(name = "reg_at")
    private OffsetDateTime regAt;

    @ColumnDefault("now()")
    @Column(name = "mod_at")
    private OffsetDateTime modAt;


}