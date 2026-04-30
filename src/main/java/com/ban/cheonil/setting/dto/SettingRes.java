package com.ban.cheonil.setting.dto;

import com.ban.cheonil.setting.entity.Setting;
import com.ban.cheonil.setting.entity.SettingCode;
import java.time.OffsetDateTime;
import java.util.Map;

public record SettingRes(
    SettingCode code,
    Map<String, Object> defaultConfig,
    Map<String, Object> userConfig,
    Map<String, Object> effectiveConfig,
    OffsetDateTime modAt) {
  public static SettingRes from(Setting s) {
    return new SettingRes(
        s.getCode(),
        s.getDefaultConfig(),
        s.getUserConfig(),
        s.getEffectiveConfig(),
        s.getModAt());
  }
}
