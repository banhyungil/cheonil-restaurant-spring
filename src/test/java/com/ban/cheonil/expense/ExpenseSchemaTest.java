package com.ban.cheonil.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ban.cheonil.expense.entity.Expense;
import com.ban.cheonil.expense.entity.ExpenseProduct;

/**
 * V6__expense_schema 검증 — 엔티티 매핑(ddl-auto=validate) + 유니크 제약.
 *
 * <p>실 PostgreSQL (Testcontainers) 사용 — NULLS NOT DISTINCT, 표현식 부분 인덱스 등 PG specific 동작 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ExpenseSchemaTest {

  @Container
  static PostgreSQLContainer pg = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  static final ZoneOffset KST = ZoneOffset.ofHours(9);

  @Autowired ExpenseRepo expenseRepo;
  @Autowired ExpenseProductRepo expenseProductRepo;

  @Test
  @DisplayName("같은 매장 + 같은 KST 일자 지출 → 중복 차단")
  void rejectSameStoreSameDay() {
    expenseRepo.saveAndFlush(makeExpense((short) 1, OffsetDateTime.of(2026, 10, 5, 0, 30, 0, 0, KST)));

    // UTC 로는 전날(10/4 15:30Z) 과 당일(10/5 14:30Z) 이지만 KST 로는 둘 다 10/5
    assertThatThrownBy(
            () ->
                expenseRepo.saveAndFlush(
                    makeExpense((short) 1, OffsetDateTime.of(2026, 10, 5, 23, 30, 0, 0, KST))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("같은 매장이라도 KST 일자가 다르면 허용")
  void allowSameStoreDifferentDay() {
    expenseRepo.saveAndFlush(makeExpense((short) 1, OffsetDateTime.of(2026, 10, 5, 23, 30, 0, 0, KST)));
    expenseRepo.saveAndFlush(makeExpense((short) 1, OffsetDateTime.of(2026, 10, 6, 0, 30, 0, 0, KST)));

    assertThat(expenseRepo.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("매장 없는 지출(공과금 등)은 같은 일자여도 허용")
  void allowNoStoreSameDay() {
    var at = OffsetDateTime.of(2026, 10, 5, 12, 0, 0, 0, KST);
    expenseRepo.saveAndFlush(makeExpense(null, at));
    expenseRepo.saveAndFlush(makeExpense(null, at));

    assertThat(expenseRepo.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("같은 제품 + 같은 단위수량 품목 → 중복 차단 (NULL 끼리도 중복)")
  void rejectDuplicateProductLine() {
    var exps = expenseRepo.saveAndFlush(makeExpense(null, OffsetDateTime.now(KST)));
    expenseProductRepo.saveAndFlush(makeLine(exps.getSeq(), null));

    assertThatThrownBy(() -> expenseProductRepo.saveAndFlush(makeLine(exps.getSeq(), null)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("같은 제품이라도 단위수량이 다르면 허용 + 소수 단위수량 저장")
  void allowSameProductDifferentUnitCnt() {
    var exps = expenseRepo.saveAndFlush(makeExpense(null, OffsetDateTime.now(KST)));
    expenseProductRepo.saveAndFlush(makeLine(exps.getSeq(), new BigDecimal("600")));
    var line = expenseProductRepo.saveAndFlush(makeLine(exps.getSeq(), new BigDecimal("1.8")));

    assertThat(expenseProductRepo.count()).isEqualTo(2);
    assertThat(expenseProductRepo.findById(line.getSeq()).orElseThrow().getUnitCnt())
        .isEqualByComparingTo("1.8");
  }

  private Expense makeExpense(Short storeSeq, OffsetDateTime expenseAt) {
    var e = new Expense();
    e.setCtgSeq(1);
    e.setStoreSeq(storeSeq);
    e.setNm("지출");
    e.setAmount(10_000);
    e.setExpenseAt(expenseAt);
    return e;
  }

  private ExpenseProduct makeLine(Long expsSeq, BigDecimal unitCnt) {
    var p = new ExpenseProduct();
    p.setExpsSeq(expsSeq);
    p.setPrdSeq(1);
    p.setCnt((short) 2);
    p.setPrice(5_000);
    p.setUnitCnt(unitCnt);
    return p;
  }
}
