package com.ban.cheonil.menu.entity;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.persistence.*;
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
@Table(name = "m_menu")
public class Menu {
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

  @Size(max = 10)
  @Column(name = "nm_s", length = 10)
  private String nmS;

  @NotNull
  @Column(name = "price", nullable = false)
  private Integer price;

  @Size(max = 1000)
  @Column(name = "cmt", length = 1000)
  private String cmt;

  @NotNull
  @ColumnDefault("true")
  @Column(name = "active", nullable = false)
  private Boolean active;

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
