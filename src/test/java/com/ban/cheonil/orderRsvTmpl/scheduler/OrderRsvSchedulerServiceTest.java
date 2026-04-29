package com.ban.cheonil.orderRsvTmpl.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ban.cheonil.orderRsvTmpl.OrderRsvTmplRepo;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@link OrderRsvSchedulerService} 단위 테스트.
 *
 * <p>외부 의존 (Repo, Creator, Clock) 모두 mock — orchestration 로직 (window 계산, 템플릿 iterate, 예외 분류) 만 검증.
 */
@ExtendWith(MockitoExtension.class)
class OrderRsvSchedulerServiceTest {

  // @Mock
  // → Mockito 가 가짜(mock) 객체를 자동 생성해 필드에 주입.
  //   실제 DB / 빈 호출 없이 stub 으로 동작 흉내만 냄.
  //   given(...).willReturn(...) 로 반환값을 지정해 사용.
  @Mock OrderRsvTmplRepo tmplRepo;
  @Mock OrderRsvCreator creator;

  /** 테스트 시계 — 한국 시간 2026-04-27 (월요일) 11:30 고정. */
  private final Clock clock =
      Clock.fixed(
          OffsetDateTime.parse("2026-04-27T11:30:00+09:00").toInstant(), ZoneId.of("Asia/Seoul"));

  OrderRsvSchedulerService orsService;

  // @BeforeEach
  // → 각 @Test 메서드 실행 직전에 매번 호출. 테스트 간 상태 격리를 위해 SUT 를 새로 만든다.
  //   (참고: @BeforeAll 은 클래스당 1회 — 공용 무거운 셋업에 사용)
  @BeforeEach
  void setUp() {
    orsService = new OrderRsvSchedulerService(tmplRepo, creator, clock);
  }

  /** windowStart = 12:30, windowEnd = 12:40 (= now + 60min ~ now + 70min). */
  private final OffsetDateTime windowStart = OffsetDateTime.parse("2026-04-27T12:30:00+09:00");

  private final OffsetDateTime windowEnd = windowStart.plusMinutes(10);

  // @Nested
  // → 테스트를 시나리오별로 그룹핑. 결과 리포트에서 "정상 흐름 > 매칭 템플릿 0개..." 처럼 트리 구조로 표시됨.
  // @DisplayName
  // → 메서드/클래스명 대신 보여줄 한글 설명. 가독성용.
  @Nested
  @DisplayName("정상 흐름")
  class HappyPath {

    // @Test → 이 메서드가 테스트라고 JUnit 에 알리는 마커.
    @Test
    @DisplayName("매칭 템플릿 0개면 0 반환 + creator 호출 없음")
    void empty() {
      // === Given (준비) ===
      // given(...).willReturn(...) — Mockito 의 BDD 스타일 stubbing.
      // "tmplRepo.findActiveInWindow 가 어떤 인자(any())로 호출되든 빈 리스트 반환" 으로 설정.
      // any() 는 "어떤 값이든 매칭" 하는 ArgumentMatcher.
      given(tmplRepo.findActiveInWindow(any(), any(), any(), any())).willReturn(List.of());

      // === When (실행) ===
      // 실제 테스트 대상 메서드 호출.
      int created = orsService.generateForWindow(windowStart, windowEnd);

      // === Then (검증) ===
      // assertThat — AssertJ 의 fluent assertion. 실패 시 가독성 좋은 메시지 출력.
      // .isZero() == .isEqualTo(0)
      assertThat(created).isZero();

      // verify — 해당 mock 메서드가 "몇 번" 호출됐는지 검증.
      // times(0) = 호출되지 않았음을 보장. (== never())
      // 빈 리스트가 반환됐으니 creator.create 가 호출될 일이 없어야 함.
      verify(creator, times(0)).create(any(), any());
    }

    @Test
    @DisplayName("매칭 템플릿 3개 → 모두 생성 시 3 반환")
    void allCreated() {
      var t1 = mockTmpl(1, LocalTime.of(12, 30));
      var t2 = mockTmpl(2, LocalTime.of(12, 35));
      var t3 = mockTmpl(3, LocalTime.of(12, 40));
      given(tmplRepo.findActiveInWindow(any(), any(), any(), any()))
          .willReturn(List.of(t1, t2, t3));
      given(creator.create(any(), any())).willReturn(true);

      int created = orsService.generateForWindow(windowStart, windowEnd);

      assertThat(created).isEqualTo(3);
      verify(creator, times(3)).create(any(), any());
    }

    @Test
    @DisplayName("일부 이미 존재 (creator.create=false) → 새로 생성된 수만 반환")
    void someAlreadyExist() {
      // var: 지역 변수 타입 추론 키워드, 우변 타입에 따라 타입이 확정된다
      // 타입이 최초 확정되면 정적 타입으로 처리되므로 추후 타입은 변경 불가하다
      var t1 = mockTmpl(1, LocalTime.of(12, 30));
      var t2 = mockTmpl(2, LocalTime.of(12, 35));
      var t3 = mockTmpl(3, LocalTime.of(12, 40));
      given(tmplRepo.findActiveInWindow(any(), any(), any(), any()))
          .willReturn(List.of(t1, t2, t3));
      given(creator.create(eq(t1), any())).willReturn(true);
      given(creator.create(eq(t2), any())).willReturn(false); // 이미 존재
      given(creator.create(eq(t3), any())).willReturn(true);

      int created = orsService.generateForWindow(windowStart, windowEnd);

      assertThat(created).isEqualTo(2);
    }

    @Test
    @DisplayName("rsvAt 은 (오늘 + 템플릿 rsv_time) 으로 정확히 계산")
    void rsvAtComputation() {
      // rsvTime: 12:33
      var tmpl = mockTmpl(1, LocalTime.of(12, 33));
      given(tmplRepo.findActiveInWindow(any(), any(), any(), any())).willReturn(List.of(tmpl));
      given(creator.create(any(), any())).willReturn(true);

      orsService.generateForWindow(windowStart, windowEnd);

      // 정확히 2026-04-27T12:33+09:00 으로 호출됐는지 검증
      var expected = OffsetDateTime.parse("2026-04-27T12:33:00+09:00");
      verify(creator).create(eq(tmpl), eq(expected));
    }
  }

  @Nested
  @DisplayName("예외 처리")
  class ErrorHandling {

    @Test
    @DisplayName("한 템플릿 예외 → 다른 템플릿은 정상 진행")
    void oneFailsOthersContinue() {
      var t1 = mockTmpl(1, LocalTime.of(12, 30));
      var t2 = mockTmpl(2, LocalTime.of(12, 35));
      var t3 = mockTmpl(3, LocalTime.of(12, 40));
      given(tmplRepo.findActiveInWindow(any(), any(), any(), any()))
          .willReturn(List.of(t1, t2, t3));
      // TIP: 만약 Match가 안된 경우는?
      // 해당 stub은 기본 반환값을 반환됨. boolean 기본 반환값은 false
      given(creator.create(eq(t1), any())).willReturn(true);
      given(creator.create(eq(t2), any())).willThrow(new RuntimeException("simulated"));
      given(creator.create(eq(t3), any())).willReturn(true);

      int created = orsService.generateForWindow(windowStart, windowEnd);

      assertThat(created).isEqualTo(2);
      verify(creator, times(3)).create(any(), any()); // t3 도 호출됐는지
    }

    @Test
    @DisplayName("DataIntegrityViolation (UNIQUE 위반) 은 정상 skip")
    void duplicateViolationSkipped() {
      var tmpl = mockTmpl(1, LocalTime.of(12, 30));
      given(tmplRepo.findActiveInWindow(any(), any(), any(), any())).willReturn(List.of(tmpl));
      given(creator.create(any(), any())).willThrow(new DataIntegrityViolationException("dup"));

      int created = orsService.generateForWindow(windowStart, windowEnd);

      assertThat(created).isZero(); // 예외지만 정상 종료, 0 반환
    }
  }

  @Nested
  @DisplayName("Repo 호출 인자")
  class RepoCallArgs {

    @Test
    @DisplayName("findActiveInWindow 에 정확한 date / dayCode / 시간 전달")
    void correctArgs() {
      given(
              tmplRepo.findActiveInWindow(
                  eq(LocalDate.of(2026, 4, 27)),
                  eq("MON"),
                  eq(LocalTime.of(12, 30)),
                  eq(LocalTime.of(12, 40))))
          .willReturn(List.of());

      orsService.generateForWindow(windowStart, windowEnd);

      verify(tmplRepo)
          .findActiveInWindow(
              eq(LocalDate.of(2026, 4, 27)),
              eq("MON"),
              eq(LocalTime.of(12, 30)),
              eq(LocalTime.of(12, 40)));
    }
  }

  /** 테스트용 OrderRsvTmpl mock — 필요한 getter 만 stub. */
  private OrderRsvTmpl mockTmpl(long seq, LocalTime rsvTime) {
    var tmpl = new OrderRsvTmpl();
    tmpl.setSeq((short) seq);
    tmpl.setRsvTime(rsvTime);
    return tmpl;
  }
}
