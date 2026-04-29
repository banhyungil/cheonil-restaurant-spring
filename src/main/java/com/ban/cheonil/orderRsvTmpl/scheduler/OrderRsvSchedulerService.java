package com.ban.cheonil.orderRsvTmpl.scheduler;

import com.ban.cheonil.orderRsvTmpl.OrderRsvTmplRepo;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 예약 템플릿 스케줄러 — 시간 window 안의 활성 템플릿을 찾아 인스턴스 생성을 위임.
 *
 * <p>이 클래스 자체엔 {@code @Transactional} 없음 — 각 템플릿 처리는 {@link OrderRsvCreator} 의
 * {@code REQUIRES_NEW} 트랜잭션으로 격리됨. 한 건 실패가 나머지에 영향 X.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderRsvSchedulerService {

  private final OrderRsvTmplRepo tmplRepo;
  private final OrderRsvCreator creator;
  private final Clock clock;

  /**
   * [windowStart, windowEnd) 범위의 rsv_time 인 활성 템플릿을 모두 처리.
   *
   * @return 새로 생성된 인스턴스 수
   */
  public int generateForWindow(OffsetDateTime windowStart, OffsetDateTime windowEnd) {
    LocalDate date = windowStart.toLocalDate();
    String today = toDayCode(date.getDayOfWeek());
    LocalTime startT = windowStart.toLocalTime();
    LocalTime endT = windowEnd.toLocalTime();

    List<OrderRsvTmpl> tmpls = tmplRepo.findActiveInWindow(date, today, startT, endT);
    if (tmpls.isEmpty()) return 0;

    int createdCnt = 0;
    for (OrderRsvTmpl tmpl : tmpls) {
      // 실제 예약 시각 = 같은 날짜 + 템플릿 rsv_time
      OffsetDateTime rsvAt =
          LocalDateTime.of(date, tmpl.getRsvTime()).atZone(clock.getZone()).toOffsetDateTime();

      try {
        if (creator.create(tmpl, rsvAt)) {
          createdCnt++;
          log.info("[rsv-scheduler] created tmpl={} rsvAt={}", tmpl.getSeq(), rsvAt);
        }
      } catch (DataIntegrityViolationException dup) {
        // UNIQUE 제약 위반 — 동시성 사고 (다른 노드가 이미 생성). 정상 skip.
        log.debug("[rsv-scheduler] duplicate skipped tmpl={} rsvAt={}", tmpl.getSeq(), rsvAt);
      } catch (Exception e) {
        log.error("[rsv-scheduler] failed tmpl={} rsvAt={}", tmpl.getSeq(), rsvAt, e);
      }
    }
    return createdCnt;
  }

  /** {@link java.time.DayOfWeek} → "MON" / "TUE" 등 (PG day_type enum 값). */
  private String toDayCode(java.time.DayOfWeek dow) {
    return dow.name().substring(0, 3);
  }
}
