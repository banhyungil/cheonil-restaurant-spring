package com.ban.cheonil.setting.entity;

/**
 * 가게 운영시간 — {@link SettingCode#OPERATING_HOURS} 의 typed view.
 *
 * <p>{@code startHour}, {@code endHour} 모두 inclusive (예: 9~20 → 12개 bucket). 통계 시간대 분석에서 hour
 * iteration 범위로 사용.
 */
public record OperatingHours(int startHour, int endHour) {
  /** 설정 누락 시 fallback. */
  public static final OperatingHours DEFAULT = new OperatingHours(9, 20);
}
