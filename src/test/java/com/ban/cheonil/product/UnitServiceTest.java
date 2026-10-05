package com.ban.cheonil.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

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

import com.ban.cheonil.product.dto.UnitRes;
import com.ban.cheonil.product.dto.UnitSaveReq;
import com.ban.cheonil.product.entity.Product;

/** {@link UnitService} 검증 규칙 — 기준 단위, 삭제/단위수량 해제 차단. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(UnitService.class)
class UnitServiceTest {

  @Container
  static PostgreSQLContainer pg = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired UnitService unitService;
  @Autowired ProductRepo productRepo;

  @Test
  @DisplayName("g → kg 기준 단위 지정")
  void createWithBaseUnit() {
    UnitRes kg = create("kg", true, null, null);
    UnitRes g = create("g", true, kg.seq(), new BigDecimal("0.001"));

    assertThat(g.baseUnitSeq()).isEqualTo(kg.seq());
    assertThat(g.baseFactor()).isEqualByComparingTo("0.001");
  }

  @Test
  @DisplayName("기준 단위 없으면 환산계수는 무시")
  void ignoreFactorWithoutBase() {
    UnitRes kg = create("kg", true, null, new BigDecimal("1"));

    assertThat(kg.baseFactor()).isNull();
  }

  @Test
  @DisplayName("이름 중복 → 차단")
  void rejectDuplicateName() {
    create("kg", true, null, null);

    assertThatThrownBy(() -> create("kg", false, null, null))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("기준 단위 지정 시 환산계수 누락 → 차단")
  void rejectBaseWithoutFactor() {
    UnitRes kg = create("kg", true, null, null);

    assertThatThrownBy(() -> create("g", true, kg.seq(), null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("기준 단위 연쇄 (mg → g → kg) → 차단")
  void rejectChainedBase() {
    UnitRes kg = create("kg", true, null, null);
    UnitRes g = create("g", true, kg.seq(), new BigDecimal("0.001"));

    assertThatThrownBy(() -> create("mg", true, g.seq(), new BigDecimal("0.001")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("다른 단위의 기준 단위에 기준 단위 지정 → 차단")
  void rejectBaseOnReferencedUnit() {
    UnitRes kg = create("kg", true, null, null);
    create("g", true, kg.seq(), new BigDecimal("0.001"));
    UnitRes ton = create("ton", true, null, null);

    assertThatThrownBy(
            () ->
                unitService.update(
                    kg.seq(), new UnitSaveReq("kg", true, ton.seq(), new BigDecimal("0.001"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("사용 중인 단위 삭제 → 차단, 미사용 단위 삭제 → 성공")
  void remove() {
    UnitRes kg = create("kg", true, null, null);
    UnitRes box = create("박스", false, null, null);
    saveProduct(kg.seq(), new BigDecimal[] {});

    assertThatThrownBy(() -> unitService.remove(kg.seq()))
        .isInstanceOf(IllegalStateException.class);

    unitService.remove(box.seq());
    assertThat(unitService.findAll()).extracting(UnitRes::nm).containsExactly("kg");
  }

  @Test
  @DisplayName("기준 단위로 쓰이는 단위 삭제 → 차단")
  void rejectRemoveReferencedBase() {
    UnitRes kg = create("kg", true, null, null);
    create("g", true, kg.seq(), new BigDecimal("0.001"));

    assertThatThrownBy(() -> unitService.remove(kg.seq()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("규격 값이 있는 제품이 있으면 단위수량 해제 차단, 없으면 허용")
  void unsetIsUnitCnt() {
    UnitRes kg = create("kg", true, null, null);
    UnitRes l = create("L", true, null, null);
    saveProduct(kg.seq(), new BigDecimal[] {new BigDecimal("1.8")});
    saveProduct(l.seq(), new BigDecimal[] {});

    assertThatThrownBy(
            () -> unitService.update(kg.seq(), new UnitSaveReq("kg", false, null, null)))
        .isInstanceOf(IllegalStateException.class);

    assertThat(unitService.update(l.seq(), new UnitSaveReq("L", false, null, null)).isUnitCnt())
        .isFalse();
  }

  private UnitRes create(String nm, boolean isUnitCnt, Short baseUnitSeq, BigDecimal factor) {
    return unitService.create(new UnitSaveReq(nm, isUnitCnt, baseUnitSeq, factor));
  }

  private short prdInfoSeq = 1;

  private void saveProduct(Short unitSeq, BigDecimal[] unitCnts) {
    var p = new Product();
    p.setPrdInfoSeq(prdInfoSeq++);
    p.setUnitSeq(unitSeq);
    p.setUnitCnts(unitCnts);
    productRepo.saveAndFlush(p);
  }
}
