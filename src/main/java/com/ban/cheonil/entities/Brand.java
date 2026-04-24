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
@Table(name = "m_brand")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Short seq;

    @Column(name = "path", columnDefinition = "ltree not null")
    private Object path;

    @Size(max = 45)
    @NotNull
    @Column(name = "nm", nullable = false, length = 45)
    private String nm;

    @Size(max = 20)
    @Column(name = "nm_s", length = 20)
    private String nmS;

    @Size(max = 500)
    @Column(name = "cmt", length = 500)
    private String cmt;

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