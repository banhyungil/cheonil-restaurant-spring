package com.ban.cheonil.entities;
// t_order_rsv 존재로 M prefix를 유지

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "m_order_rsv_tmpl")
public class MOrderRsvTmpl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Short seq;

    @NotNull
    @Column(name = "store_seq", nullable = false)
    private Short storeSeq;

    @Size(max = 40)
    @NotNull
    @Column(name = "nm", nullable = false, length = 40)
    private String nm;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @NotNull
    @Column(name = "rsv_time", nullable = false)
    private LocalTime rsvTime;

    @Column(name = "day_types", columnDefinition = "day_type[] not null")
    private Object dayTypes;

    @Size(max = 1000)
    @Column(name = "cmt", length = 1000)
    private String cmt;

    @ColumnDefault("true")
    @Column(name = "active")
    private Boolean active;

    @Column(name = "start_dt")
    private LocalDate startDt;

    @Column(name = "end_dt")
    private LocalDate endDt;

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