package com.ban.cheonil.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

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

import com.ban.cheonil.expense.ExpenseProductRepo;
import com.ban.cheonil.expense.entity.ExpenseProduct;
import com.ban.cheonil.product.dto.IngredientRes;
import com.ban.cheonil.product.dto.IngredientSaveReq;
import com.ban.cheonil.product.dto.ProductRes;
import com.ban.cheonil.product.dto.ProductSaveReq;
import com.ban.cheonil.product.entity.Unit;

/** {@link ProductService} / {@link IngredientService} — 식자재·제품정보 찾기/생성, 공유·정리, 삭제 제약. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ProductService.class, IngredientService.class})
class ProductServiceTest {

  @Container
  static PostgreSQLContainer pg = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired ProductService productService;
  @Autowired IngredientService ingredientService;
  @Autowired UnitRepo unitRepo;
  @Autowired ProductInfoRepo productInfoRepo;
  @Autowired ExpenseProductRepo expenseProductRepo;

  Short kg;
  Short g;
  Short box;

  @BeforeEach
  void setUp() {
    kg = saveUnit("kg", true);
    g = saveUnit("g", true);
    box = saveUnit("박스", false);
  }

  @Test
  @DisplayName("생성 — 없는 식자재/제품정보는 자동 생성, 이름은 trim")
  void createWithNewIngredient() {
    ProductRes p = create(" 쌀 ", "경기미 추청 ", kg, List.of(dec("20")));

    assertThat(p.ingdNm()).isEqualTo("쌀");
    assertThat(p.nm()).isEqualTo("경기미 추청");
    assertThat(p.unitNm()).isEqualTo("kg");
    assertThat(ingredientService.findAll())
        .extracting(IngredientRes::nm, IngredientRes::productCnt)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("쌀", 1L));
  }

  @Test
  @DisplayName("같은 제품명의 다른 단위 → 제품정보 공유, 같은 단위 → 중복 차단")
  void shareInfoAndRejectDuplicate() {
    ProductRes perG = create("삼겹살", "삼겹살", g, List.of());
    ProductRes perKg = create("삼겹살", "삼겹살", kg, List.of());

    assertThat(perKg.prdInfoSeq()).isEqualTo(perG.prdInfoSeq());
    assertThatThrownBy(() -> create("삼겹살", "삼겹살", g, List.of()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("규격 — scale 통일 후 중복 제거·정렬, 단위수량 미사용 단위면 비움")
  void normalizeUnitCnts() {
    ProductRes p = create("삼겹살", "삼겹살", g, List.of(dec("600"), dec("500"), dec("600.0")));
    ProductRes b = create("계란", "계란", box, List.of(dec("30")));

    assertThat(p.unitCnts()).containsExactly(dec("500.00"), dec("600.00"));
    assertThat(b.unitCnts()).isEmpty();
  }

  @Test
  @DisplayName("수정 — 제품명 변경 시 혼자 쓰던 제품정보는 정리, 공유 중이면 유지")
  void updateCleansOrphanInfo() {
    ProductRes alone = create("당면", "당면", kg, List.of());
    ProductRes renamed = update(alone.seq(), "당면", "오뚜기 알뜰당면", kg);

    assertThat(renamed.prdInfoSeq()).isNotEqualTo(alone.prdInfoSeq());
    assertThat(productInfoRepo.existsById(alone.prdInfoSeq())).isFalse();

    ProductRes perG = create("삼겹살", "삼겹살", g, List.of());
    create("삼겹살", "삼겹살", kg, List.of());
    update(perG.seq(), "삼겹살", "국내산 삼겹살", g);

    assertThat(productInfoRepo.existsById(perG.prdInfoSeq())).isTrue();
  }

  @Test
  @DisplayName("삭제 — 지출에서 사용 중이면 차단, 아니면 삭제 + 제품정보 정리")
  void remove() {
    ProductRes used = create("쌀", "경기미", kg, List.of());
    ProductRes unused = create("당면", "당면", kg, List.of());
    saveExpenseLine(used.seq());

    assertThatThrownBy(() -> productService.remove(used.seq()))
        .isInstanceOf(IllegalStateException.class);

    productService.remove(unused.seq());
    assertThat(productInfoRepo.existsById(unused.prdInfoSeq())).isFalse();
    assertThat(productService.findAll()).extracting(ProductRes::nm).containsExactly("경기미");
  }

  @Test
  @DisplayName("식자재 — 이름 중복 차단, 제품 있으면 삭제 차단, 없으면 삭제")
  void ingredient() {
    create("쌀", "경기미", kg, List.of());
    IngredientRes salt = ingredientService.create(new IngredientSaveReq("소금"));
    Short riceSeq = ingredientService.findAll().get(1).seq(); // 소금, 쌀 순

    assertThatThrownBy(() -> ingredientService.create(new IngredientSaveReq("쌀")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> ingredientService.update(salt.seq(), new IngredientSaveReq("쌀")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> ingredientService.remove(riceSeq))
        .isInstanceOf(IllegalStateException.class);

    ingredientService.remove(salt.seq());
    assertThat(ingredientService.findAll()).extracting(IngredientRes::nm).containsExactly("쌀");
  }

  private ProductRes create(String ingdNm, String nm, Short unitSeq, List<BigDecimal> cnts) {
    return productService.create(new ProductSaveReq(ingdNm, nm, unitSeq, cnts));
  }

  private ProductRes update(Integer seq, String ingdNm, String nm, Short unitSeq) {
    return productService.update(seq, new ProductSaveReq(ingdNm, nm, unitSeq, List.of()));
  }

  private Short saveUnit(String nm, boolean isUnitCnt) {
    var u = new Unit();
    u.setNm(nm);
    u.setIsUnitCnt(isUnitCnt);
    return unitRepo.save(u).getSeq();
  }

  private void saveExpenseLine(Integer prdSeq) {
    var l = new ExpenseProduct();
    l.setExpsSeq(1L);
    l.setPrdSeq(prdSeq);
    l.setCnt((short) 1);
    l.setPrice(1000);
    expenseProductRepo.saveAndFlush(l);
  }

  private static BigDecimal dec(String v) {
    return new BigDecimal(v);
  }
}
