package com.ban.cheonil.orderRsvTmpl.scheduler;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ban.cheonil.setting.SettingService;
import com.ban.cheonil.setting.entity.RsvScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 예약 템플릿 → 인스턴스 자동 생성 cron.
 *
 * <p>매 10분 정각 실행. 각 trigger 는 {@code [now+leadMinutes, now+leadMinutes+10min)} 범위의 rsv_time 인
 * 템플릿을 처리. 즉 각 예약 인스턴스는 rsv_time 약 leadMinutes ~ (leadMinutes+10) 분 전에 미리 생성됨.
 *
 * <p>{@code leadMinutes} 는 설정 화면(RSV_SCHEDULER)에서 조정 가능 — 매 tick 마다 최신 값 적용.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRsvSchedulerJob {

  /** Window 폭 (분) — cron 주기와 일치해야 누락 없음. */
  private static final int WINDOW_MINUTES = 10;

  private final OrderRsvSchedulerService service;
  private final SettingService settingService;
  private final Clock clock;

  @Scheduled(cron = "0 */10 * * * *")
  public void tick() {
    RsvScheduler config = settingService.getRsvScheduler();
    OffsetDateTime now = OffsetDateTime.now(clock).withSecond(0).withNano(0);
    OffsetDateTime windowStart = now.plusMinutes(config.leadMinutes());
    OffsetDateTime windowEnd = windowStart.plusMinutes(WINDOW_MINUTES);

    int created = service.generateForWindow(windowStart, windowEnd);
    if (created > 0) {
      log.info(
          "[rsv-scheduler] tick created={} window=[{}, {}) lead={}min",
          created,
          windowStart,
          windowEnd,
          config.leadMinutes());
    }
  }
}
