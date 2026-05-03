package com.ban.cheonil.orderRsvTmpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;

/**
 * {@link OrderRsvTmplRepo#findActiveInWindow} JPA 슬라이스 테스트.
 *
 * <p>실 PostgreSQL (Testcontainers) 사용 — day_type[] enum array, array_position 등 PG specific 동작 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // H2 자동 대체 막기
@Testcontainers
class OrderRsvTmplRepoTest {

  // Flyway 가 V1__init.sql 에서 enum/cast/function/extension + 테이블까지 모두 생성.
  // (이전에는 withInitScript("init-pg.sql") + ddl-auto=update 로 두 단계 처리)
  @Container
  static PostgreSQLContainer pg = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired OrderRsvTmplRepo repo;
  @Autowired EntityManager em;

  @Test
  @DisplayName("active=true + window 내 시간 + 오늘 요일 포함 + 날짜 범위 안 → 매칭")
  void matchesAllConditions() {
    save(
        makeTmpl(
            (short) 1,
            true,
            LocalTime.of(12, 30),
            new String[] {"MON", "WED"},
            LocalDate.of(2026, 1, 1),
            null));
    em.flush();
    em.clear();

    var date = LocalDate.of(2026, 4, 27); // 월요일
    var result = repo.findActiveInWindow(date, "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getNm()).isEqualTo("tmpl-1");
  }

  @Test
  @DisplayName("active=false → 제외")
  void excludeInactive() {
    save(
        makeTmpl(
            (short) 2,
            false,
            LocalTime.of(12, 30),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            null));
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("day_types 에 오늘 요일 미포함 → 제외")
  void excludeWrongDay() {
    save(
        makeTmpl(
            (short) 3,
            true,
            LocalTime.of(12, 30),
            new String[] {"TUE"},
            LocalDate.of(2026, 1, 1),
            null));
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("rsv_time 이 window 밖 → 제외")
  void excludeOutOfWindow() {
    save(
        makeTmpl(
            (short) 4,
            true,
            LocalTime.of(11, 0),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            null));
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("end_dt 가 과거 → 제외")
  void excludeEnded() {
    save(
        makeTmpl(
            (short) 5,
            true,
            LocalTime.of(12, 30),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 1))); // end_dt 4/1
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("end_dt = NULL (무기한) → 매칭")
  void includeOpenEnded() {
    save(
        makeTmpl(
            (short) 6,
            true,
            LocalTime.of(12, 30),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            null));
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("start_dt 가 미래 → 제외")
  void excludeNotStarted() {
    save(
        makeTmpl(
            (short) 7,
            true,
            LocalTime.of(12, 30),
            new String[] {"MON"},
            LocalDate.of(2026, 5, 1),
            null)); // start_dt 5/1
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("여러 조건 혼합 — 매칭되는 것만 반환")
  void mixed() {
    save(
        makeTmpl(
            (short) 1,
            true,
            LocalTime.of(12, 30),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            null)); // 매칭
    save(
        makeTmpl(
            (short) 2,
            false,
            LocalTime.of(12, 30),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            null)); // skip
    save(
        makeTmpl(
            (short) 3,
            true,
            LocalTime.of(13, 0),
            new String[] {"MON"},
            LocalDate.of(2026, 1, 1),
            null)); // skip (시간 밖)
    save(
        makeTmpl(
            (short) 4,
            true,
            LocalTime.of(12, 35),
            new String[] {"MON", "WED"},
            LocalDate.of(2026, 1, 1),
            null)); // 매칭
    em.flush();
    em.clear();

    var result =
        repo.findActiveInWindow(
            LocalDate.of(2026, 4, 27), "MON", LocalTime.of(12, 30), LocalTime.of(12, 40));

    assertThat(result).hasSize(2);
  }

  /* ====== helpers ====== */

  private void save(OrderRsvTmpl t) {
    em.persist(t);
  }

  private OrderRsvTmpl makeTmpl(
      short suffix,
      boolean active,
      LocalTime rsvTime,
      String[] dayTypes,
      LocalDate startDt,
      LocalDate endDt) {
    var t = new OrderRsvTmpl();
    t.setStoreSeq((short) 1);
    t.setNm("tmpl-" + suffix);
    t.setAmount(10000);
    t.setRsvTime(rsvTime);
    t.setDayTypes(dayTypes);
    t.setActive(active);
    t.setAutoOrder(false);
    t.setStartDt(startDt);
    t.setEndDt(endDt);
    return t;
  }
}
