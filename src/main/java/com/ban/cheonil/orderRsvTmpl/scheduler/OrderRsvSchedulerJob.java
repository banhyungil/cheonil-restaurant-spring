package com.ban.cheonil.orderRsvTmpl.scheduler;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 예약 템플릿 → 인스턴스 자동 생성 cron.
 *
 * <p>매 10분 정각 실행. 각 trigger 는 {@code [now+60min, now+70min)} 범위의 rsv_time 인 템플릿을 처리.
 * 즉 각 예약 인스턴스는 rsv_time 약 60~70분 전에 미리 생성됨.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRsvSchedulerJob {

  /** 트리거 시점 기준 — 예약 시각이 얼마나 미래인지 (분). */
  private static final int LEAD_MINUTES = 60;

  /** Window 폭 (분) — cron 주기와 일치해야 누락 없음. */
  private static final int WINDOW_MINUTES = 10;

  private final OrderRsvSchedulerService service;

  @Scheduled(cron = "0 */10 * * * *")
  public void tick() {
    OffsetDateTime now = OffsetDateTime.now().withSecond(0).withNano(0);
    OffsetDateTime windowStart = now.plusMinutes(LEAD_MINUTES);
    OffsetDateTime windowEnd = windowStart.plusMinutes(WINDOW_MINUTES);

    int created = service.generateForWindow(windowStart, windowEnd);
    if (created > 0) {
      log.info(
          "[rsv-scheduler] tick created={} window=[{}, {})",
          created,
          windowStart,
          windowEnd);
    }
  }
}
