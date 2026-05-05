package com.ban.cheonil.setting;

import com.ban.cheonil.setting.dto.SettingRes;
import com.ban.cheonil.setting.entity.OperatingHours;
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

  /**
   * 가게 운영시간 — typed view. setting row 누락 / config 필드 누락 시 {@link OperatingHours#DEFAULT} fallback.
   *
   * <p>backend 가 직접 소비하는 setting 이라 raw Map cast 보일러플레이트 회피를 위해 typed 헬퍼 노출.
   */
  public OperatingHours getOperatingHours() {
    return settingRepo
        .findById(SettingCode.OPERATING_HOURS)
        .map(
            s -> {
              Map<String, Object> cfg = s.getEffectiveConfig();
              if (cfg == null) return OperatingHours.DEFAULT;
              Object start = cfg.get("startHour");
              Object end = cfg.get("endHour");
              if (!(start instanceof Number) || !(end instanceof Number))
                return OperatingHours.DEFAULT;
              return new OperatingHours(((Number) start).intValue(), ((Number) end).intValue());
            })
        .orElse(OperatingHours.DEFAULT);
  }

  private Setting get(SettingCode code) {
    return settingRepo
        .findById(code)
        .orElseThrow(() -> new EntityNotFoundException("setting " + code + " not found"));
  }
}
