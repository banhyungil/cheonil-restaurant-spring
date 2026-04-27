package com.ban.cheonil.orderRsvTmpl.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_order_rsv_tmpl")
public class OrderRsvTmpl {
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

  /**
   * PostgreSQL custom enum array {@code day_type[]}. JPA 가 enum array 를 직접 매핑하지 못해 String[] 로 받고
   * DTO 변환 시점에 {@link DayType} 으로 변환.
   */
  @NotNull
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "day_types", nullable = false, columnDefinition = "day_type[]")
  private String[] dayTypes;

  @Size(max = 1000)
  @Column(name = "cmt", length = 1000)
  private String cmt;

  @ColumnDefault("true")
  @Column(name = "active")
  private Boolean active;

  @NotNull
  @ColumnDefault("(now())::date")
  @Column(name = "start_dt", nullable = false)
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
