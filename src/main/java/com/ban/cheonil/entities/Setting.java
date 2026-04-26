package com.ban.cheonil.entities;

import java.util.Map;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_setting")
public class Setting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Short seq;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "config", nullable = false)
  private Map<String, Object> config;
}
