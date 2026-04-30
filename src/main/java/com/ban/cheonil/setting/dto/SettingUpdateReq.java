package com.ban.cheonil.setting.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SettingUpdateReq(@NotNull Map<String, Object> userConfig) {}
