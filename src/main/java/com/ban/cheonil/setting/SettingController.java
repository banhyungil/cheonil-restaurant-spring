package com.ban.cheonil.setting;

import com.ban.cheonil.setting.dto.SettingRes;
import com.ban.cheonil.setting.dto.SettingUpdateReq;
import com.ban.cheonil.setting.entity.SettingCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingController {

  private final SettingService settingService;

  @GetMapping
  public List<SettingRes> list() {
    return settingService.findAll();
  }

  @GetMapping("/{code}")
  public SettingRes get(@PathVariable SettingCode code) {
    return settingService.findByCode(code);
  }

  /** user_config 갱신. */
  @PutMapping("/{code}")
  public SettingRes update(
      @PathVariable SettingCode code, @Valid @RequestBody SettingUpdateReq req) {
    return settingService.updateUserConfig(code, req.userConfig());
  }

  /** user_config 를 NULL 로 → default 복원. */
  @PostMapping("/{code}/restore")
  public SettingRes restore(@PathVariable SettingCode code) {
    return settingService.restore(code);
  }
}
