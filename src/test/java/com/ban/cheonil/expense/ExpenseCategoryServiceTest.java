package com.ban.cheonil.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ban.cheonil.expense.dto.ExpenseCategoryRes;
import com.ban.cheonil.expense.dto.ExpenseCategorySaveReq;
import com.ban.cheonil.expense.entity.Expense;

/** {@link ExpenseCategoryService} — ltree path 생성/이동 + 검증 규칙. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(ExpenseCategoryService.class)
class ExpenseCategoryServiceTest {

  @Container
  static PostgreSQLContainer pg = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired ExpenseCategoryService service;
  @Autowired ExpenseRepo expenseRepo;

  @Test
  @DisplayName("생성 — path 는 상위 path + 자기 seq, parentSeq 계산")
  void createPath() {
    var food = create(null, "식자재");
    var veg = create(food.seq(), "채소");
    var leaf = create(veg.seq(), "잎채소");

    assertThat(food.path()).isEqualTo(String.valueOf(food.seq()));
    assertThat(food.parentSeq()).isNull();
    assertThat(veg.path()).isEqualTo(food.seq() + "." + veg.seq());
    assertThat(leaf.path()).isEqualTo(food.seq() + "." + veg.seq() + "." + leaf.seq());
    assertThat(leaf.parentSeq()).isEqualTo(veg.seq());
  }

  @Test
  @DisplayName("이동 — 하위 트리째 다른 상위로, 이름도 함께 변경")
  void moveSubtree() {
    var food = create(null, "식자재");
    var ops = create(null, "운영비");
    var veg = create(food.seq(), "채소");
    var leaf = create(veg.seq(), "잎채소");

    var moved = service.update(veg.seq(), new ExpenseCategorySaveReq(ops.seq(), "야채"));

    assertThat(moved.path()).isEqualTo(ops.seq() + "." + veg.seq());
    assertThat(moved.nm()).isEqualTo("야채");
    assertThat(byseq(leaf.seq()).path()).isEqualTo(ops.seq() + "." + veg.seq() + "." + leaf.seq());
  }

  @Test
  @DisplayName("이동 — 최상위로")
  void moveToRoot() {
    var food = create(null, "식자재");
    var veg = create(food.seq(), "채소");
    var leaf = create(veg.seq(), "잎채소");

    service.update(veg.seq(), new ExpenseCategorySaveReq(null, "채소"));

    assertThat(byseq(veg.seq()).path()).isEqualTo(String.valueOf(veg.seq()));
    assertThat(byseq(veg.seq()).parentSeq()).isNull();
    assertThat(byseq(leaf.seq()).path()).isEqualTo(veg.seq() + "." + leaf.seq());
  }

  @Test
  @DisplayName("자기 자신 / 하위로 이동 → 차단")
  void rejectMoveIntoSelf() {
    var food = create(null, "식자재");
    var veg = create(food.seq(), "채소");

    assertThatThrownBy(
            () -> service.update(food.seq(), new ExpenseCategorySaveReq(veg.seq(), "식자재")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> service.update(food.seq(), new ExpenseCategorySaveReq(food.seq(), "식자재")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("같은 상위 안 이름 중복 → 차단, 다른 상위면 허용")
  void siblingNm() {
    var food = create(null, "식자재");
    var ops = create(null, "운영비");
    create(food.seq(), "기타");

    assertThatThrownBy(() -> create(food.seq(), "기타")).isInstanceOf(IllegalStateException.class);
    assertThat(create(ops.seq(), "기타").parentSeq()).isEqualTo(ops.seq());
  }

  @Test
  @DisplayName("삭제 — 하위 있거나 지출 연결 시 차단, 말단은 삭제")
  void remove() {
    var food = create(null, "식자재");
    var veg = create(food.seq(), "채소");
    var meat = create(food.seq(), "육류");
    saveExpense(meat.seq());

    assertThatThrownBy(() -> service.remove(food.seq())).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> service.remove(meat.seq())).isInstanceOf(IllegalStateException.class);

    service.remove(veg.seq());
    assertThat(service.findAll()).extracting(ExpenseCategoryRes::nm).containsExactly("식자재", "육류");
  }

  private ExpenseCategoryRes create(Integer parentSeq, String nm) {
    return service.create(new ExpenseCategorySaveReq(parentSeq, nm));
  }

  private ExpenseCategoryRes byseq(Integer seq) {
    return service.findAll().stream().filter(c -> c.seq().equals(seq)).findFirst().orElseThrow();
  }

  private void saveExpense(Integer ctgSeq) {
    var e = new Expense();
    e.setCtgSeq(ctgSeq);
    e.setNm("지출");
    e.setAmount(10_000);
    e.setExpenseAt(OffsetDateTime.now());
    expenseRepo.saveAndFlush(e);
  }
}
