package com.ban.cheonil.store.entity;

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
@Table(name = "m_store")
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Short seq;

    @NotNull
    @Column(name = "ctg_seq", nullable = false)
    private Short ctgSeq;

    @Size(max = 45)
    @NotNull
    @Column(name = "nm", nullable = false, length = 45)
    private String nm;

    @Size(max = 200)
    @Column(name = "addr", length = 200)
    private String addr;

    @Size(max = 1000)
    @Column(name = "cmt", length = 1000)
    private String cmt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

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