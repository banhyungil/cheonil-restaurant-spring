package com.ban.cheonil.setting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "m_setting")
public class Setting {

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "code", nullable = false, length = 40)
  private SettingCode code;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_config", nullable = false)
  private Map<String, Object> defaultConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "user_config")
  private Map<String, Object> userConfig;

  @ColumnDefault("now()")
  @Column(name = "mod_at")
  private OffsetDateTime modAt;

  /** user_config 가 있으면 그것을, 없으면 default_config 반환. */
  public Map<String, Object> getEffectiveConfig() {
    return userConfig != null ? userConfig : defaultConfig;
  }
}
