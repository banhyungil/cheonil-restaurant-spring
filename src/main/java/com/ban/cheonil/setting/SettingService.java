package com.ban.cheonil.setting;

import com.ban.cheonil.setting.dto.SettingRes;
import com.ban.cheonil.setting.entity.Setting;
import com.ban.cheonil.setting.entity.SettingCode;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingService {

  private final SettingRepo settingRepo;

  public List<SettingRes> findAll() {
    return settingRepo.findAll().stream().map(SettingRes::from).toList();
  }

  public SettingRes findByCode(SettingCode code) {
    return SettingRes.from(get(code));
  }

  /** user_config 갱신. mod_at 자동 업데이트. */
  @Transactional
  public SettingRes updateUserConfig(SettingCode code, Map<String, Object> userConfig) {
    Setting s = get(code);
    s.setUserConfig(userConfig);
    s.setModAt(OffsetDateTime.now());
    return SettingRes.from(s);
  }

  /** user_config 를 NULL 로 클리어 → default 로 복원. */
  @Transactional
  public SettingRes restore(SettingCode code) {
    Setting s = get(code);
    s.setUserConfig(null);
    s.setModAt(OffsetDateTime.now());
    return SettingRes.from(s);
  }

  private Setting get(SettingCode code) {
    return settingRepo
        .findById(code)
        .orElseThrow(() -> new EntityNotFoundException("setting " + code + " not found"));
  }
}
