package com.ban.cheonil.setting;

import com.ban.cheonil.setting.entity.Setting;
import com.ban.cheonil.setting.entity.SettingCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepo extends JpaRepository<Setting, SettingCode> {}
