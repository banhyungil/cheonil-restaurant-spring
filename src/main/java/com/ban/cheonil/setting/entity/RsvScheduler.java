package com.ban.cheonil.setting.entity;

/**
 * 예약 스케줄러 파라미터 — {@link SettingCode#RSV_SCHEDULER} 의 typed view.
 *
 * <p>{@code leadMinutes} = 트리거 시점 기준 몇 분 미래의 rsv_time 을 처리할지. cron 주기(10분) 이상이어야 누락 없음.
 */
public record RsvScheduler(int leadMinutes) {
  /** 설정 누락 시 fallback. */
  public static final RsvScheduler DEFAULT = new RsvScheduler(60);
}
