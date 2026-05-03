package com.ban.cheonil.orderRsvTmpl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
   * PostgreSQL custom enum array {@code day_type[]}.
   *
   * <p>Hibernate 가 enum array 를 PG custom enum array 로 직접 매핑 못 함 (ordinal smallint[] 으로 직렬화 시도 →
   * PSQL ERROR: "expression is of type smallint[]"). String[] 로 받고 DTO 변환 시점에 {@link DayType} 으로
   * 변환. 추후 hypersistence-utils 도입 시 DayType[] 직접 매핑 가능. DB CAST 문 추가: CREATE CAST (varchar[] AS
   * day_type[]) WITH INOUT AS IMPLICIT;
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

  /** true 면 스케줄러가 예약 생성 시 주문(t_order)도 즉시 생성. 신뢰 단골에 한해 활성화. */
  @NotNull
  @ColumnDefault("false")
  @Column(name = "auto_order", nullable = false)
  private Boolean autoOrder;

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

/**
 * "@Column"(nullable = false) (JPA)
 *
 * <p>DDL 생성 시 DB 스키마 레벨 "@NotNull" (Bean Validation - jakarta.validation)
 *
 * <p>엔티티 저장/수정 전 애플리케이션 레벨 검증 ConstraintViolationException 발생 빠른 실패 + 명확한 에러 메시지
 *
 * <p>결론: 둘 다 쓰는 게 베스트 프랙티스.
 */
