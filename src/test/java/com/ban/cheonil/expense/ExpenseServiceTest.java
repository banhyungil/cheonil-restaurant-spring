package com.ban.cheonil.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
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

import com.ban.cheonil.expense.dto.ExpenseCategorySaveReq;
import com.ban.cheonil.expense.dto.ExpenseRes;
import com.ban.cheonil.expense.dto.ExpenseSaveReq;
import com.ban.cheonil.expense.dto.ExpensesParams;
import com.ban.cheonil.expense.entity.ExpenseProduct;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

/** {@link ExpenseService} — 기간/카테고리(하위 포함) 조회, 같은 일자·매장 차단, lookup, 지출명 추천, 삭제. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ExpenseService.class, ExpenseCategoryService.class})
class ExpenseServiceTest {

  @Container
  static PostgreSQLContainer pg = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  static final LocalDate D1 = LocalDate.of(2026, 10, 1);
  static final LocalDate D2 = LocalDate.of(2026, 10, 2);

  @Autowired ExpenseService service;
  @Autowired ExpenseCategoryService ctgService;
  @Autowired StoreRepo storeRepo;
  @Autowired ExpenseProductRepo expenseProductRepo;

  Integer food;
  Integer veg;
  Integer utility;
  Short nh;

  @BeforeEach
  void setUp() {
    food = ctgService.create(new ExpenseCategorySaveReq(null, "식자재")).seq();
    veg = ctgService.create(new ExpenseCategorySaveReq(food, "채소")).seq();
    utility = ctgService.create(new ExpenseCategorySaveReq(null, "공과금")).seq();
    nh = saveStore("농협");
  }

  @Test
  @DisplayName("일자는 KST 일자 그대로 저장·조회된다")
  void expenseDt() {
    ExpenseRes e = create(veg, nh, "대파", 10_000, D1);

    assertThat(e.expenseDt()).isEqualTo(D1);
    assertThat(service.findBySeq(e.seq()).expenseDt()).isEqualTo(D1);
  }

  @Test
  @DisplayName("조회 — 기간 경계 포함, 카테고리는 하위까지, 최신 일자순")
  void findAll() {
    create(veg, nh, "대파", 10_000, D1);
    create(food, null, "쌀", 50_000, D2);
    create(utility, null, "전기요금", 80_000, D2);
    create(utility, null, "가스요금", 30_000, D2.plusDays(1));

    assertThat(find(D1, D2, null, null, null))
        .extracting(ExpenseRes::nm)
        .containsExactly("전기요금", "쌀", "대파");
    assertThat(find(D1, D2, food, null, null))
        .extracting(ExpenseRes::nm)
        .containsExactlyInAnyOrder("쌀", "대파");
    assertThat(find(D1, D2, null, nh, null)).extracting(ExpenseRes::nm).containsExactly("대파");
    assertThat(find(D1, D2, null, null, "요금")).extracting(ExpenseRes::nm).containsExactly("전기요금");
  }

  @Test
  @DisplayName("같은 일자 + 같은 매장 → 차단 (수정 시 자기 자신은 제외), 매장 없으면 허용")
  void sameStoreSameDay() {
    ExpenseRes e = create(veg, nh, "대파", 10_000, D1);

    assertThatThrownBy(() -> create(veg, nh, "양파", 5_000, D1))
        .isInstanceOf(IllegalStateException.class);
    assertThat(update(e.seq(), veg, nh, "대파 외", 15_000, D1).amount()).isEqualTo(15_000);
    create(utility, null, "전기요금", 80_000, D1);
    create(utility, null, "수도요금", 20_000, D1);
    assertThat(find(D1, D1, null, null, null)).hasSize(3);
  }

  @Test
  @DisplayName("lookup — 같은 일자 + 같은 매장 기존 지출, 없으면 empty")
  void lookup() {
    ExpenseRes e = create(veg, nh, "대파", 10_000, D1);

    assertThat(service.lookup(D1, nh)).map(ExpenseRes::seq).contains(e.seq());
    assertThat(service.lookup(D2, nh)).isEmpty();
  }

  @Test
  @DisplayName("지출명 추천 — 카테고리별, 중복 없이 최근 사용순")
  void recentNames() {
    create(utility, null, "가스요금", 30_000, D1);
    create(utility, null, "전기요금", 80_000, D1);
    create(utility, null, "가스요금", 31_000, D2);
    create(veg, nh, "대파", 10_000, D2);

    assertThat(service.recentNames(utility)).containsExactly("가스요금", "전기요금");
  }

  @Test
  @DisplayName("삭제 — 품목도 함께 삭제")
  void removeWithProducts() {
    ExpenseRes e = create(veg, nh, "대파", 10_000, D1);
    var line = new ExpenseProduct();
    line.setExpsSeq(e.seq());
    line.setPrdSeq(1);
    line.setCnt((short) 2);
    line.setPrice(5_000);
    expenseProductRepo.saveAndFlush(line);

    service.remove(e.seq());

    assertThat(find(D1, D1, null, null, null)).isEmpty();
    assertThat(expenseProductRepo.count()).isZero();
  }

  private ExpenseRes create(Integer ctgSeq, Short storeSeq, String nm, int amount, LocalDate dt) {
    return service.create(new ExpenseSaveReq(ctgSeq, storeSeq, nm, amount, dt, null));
  }

  private ExpenseRes update(
      Long seq, Integer ctgSeq, Short storeSeq, String nm, int amount, LocalDate dt) {
    return service.update(seq, new ExpenseSaveReq(ctgSeq, storeSeq, nm, amount, dt, null));
  }

  private java.util.List<ExpenseRes> find(
      LocalDate from, LocalDate to, Integer ctgSeq, Short storeSeq, String q) {
    return service.findAll(new ExpensesParams(from, to, ctgSeq, storeSeq, q));
  }

  private Short saveStore(String nm) {
    var s = new Store();
    s.setCtgSeq((short) 1);
    s.setNm(nm);
    s.setActive(true);
    return storeRepo.saveAndFlush(s).getSeq();
  }
}
