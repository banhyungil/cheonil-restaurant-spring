package com.ban.cheonil.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.expense.ExpenseProductRepo;
import com.ban.cheonil.product.dto.ProductRes;
import com.ban.cheonil.product.dto.ProductSaveReq;
import com.ban.cheonil.product.entity.Ingredient;
import com.ban.cheonil.product.entity.Product;
import com.ban.cheonil.product.entity.ProductInfo;
import com.ban.cheonil.product.entity.Unit;

import lombok.RequiredArgsConstructor;

/**
 * 제품 = 제품정보(식자재 + 제품명) + 단위. 화면에서는 한 덩어리로 다루고, 저장 시 식자재 / 제품정보를 찾거나 만든다.
 *
 * <p>제품정보는 같은 이름의 다른 단위 제품(삼겹살 g / kg)이 공유한다. 수정·삭제로 제품이 하나도 남지 않은 제품정보는 정리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private static final Collator KO = Collator.getInstance(Locale.KOREAN);

  private final ProductRepo productRepo;
  private final ProductInfoRepo productInfoRepo;
  private final IngredientRepo ingredientRepo;
  private final UnitRepo unitRepo;
  private final ExpenseProductRepo expenseProductRepo;

  /** 전체 제품 — 식자재명, 제품명, 단위명 순. 마스터 데이터라 전체를 읽어 Java 에서 조인한다. */
  public List<ProductRes> findAll() {
    Map<Short, ProductInfo> infos = bySeq(productInfoRepo.findAll(), ProductInfo::getSeq);
    Map<Short, Ingredient> ingds = bySeq(ingredientRepo.findAll(), Ingredient::getSeq);
    Map<Short, Unit> units = bySeq(unitRepo.findAll(), Unit::getSeq);

    return productRepo.findAll().stream()
        .map(p -> toRes(p, infos.get(p.getPrdInfoSeq()), ingds, units))
        .sorted(
            Comparator.comparing(ProductRes::ingdNm, KO)
                .thenComparing(ProductRes::nm, KO)
                .thenComparing(ProductRes::unitNm, KO))
        .toList();
  }

  @Transactional
  public ProductRes create(ProductSaveReq req) {
    Product p = new Product();
    apply(p, req);
    return toRes(productRepo.save(p));
  }

  /** 전체 교체 (PUT). 제품정보가 바뀌어 이전 제품정보에 남은 제품이 없으면 정리. */
  @Transactional
  public ProductRes update(Integer seq, ProductSaveReq req) {
    Product p = get(seq);
    Short oldInfoSeq = p.getPrdInfoSeq();
    apply(p, req);
    productRepo.flush();
    if (!oldInfoSeq.equals(p.getPrdInfoSeq())) {
      removeInfoIfOrphan(oldInfoSeq);
    }
    return toRes(p);
  }

  @Transactional
  public void remove(Integer seq) {
    Product p = get(seq);
    long cnt = expenseProductRepo.countByPrdSeq(seq);
    if (cnt > 0) {
      throw new IllegalStateException("지출 " + cnt + "건에서 사용 중인 제품이라 삭제할 수 없습니다.");
    }
    productRepo.delete(p);
    productRepo.flush();
    removeInfoIfOrphan(p.getPrdInfoSeq());
  }

  /* ==================== helpers ==================== */

  private Product get(Integer seq) {
    return productRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("product " + seq + " not found"));
  }

  private void apply(Product p, ProductSaveReq req) {
    Unit unit =
        unitRepo
            .findById(req.unitSeq())
            .orElseThrow(() -> new EntityNotFoundException("unit " + req.unitSeq() + " not found"));
    Ingredient ingd = findOrCreateIngredient(req.ingdNm().trim());
    ProductInfo info = findOrCreateInfo(ingd.getSeq(), req.nm().trim());

    if (productRepo.existsByKey(info.getSeq(), unit.getSeq(), p.getSeq())) {
      throw new IllegalStateException(
          "이미 등록된 제품입니다: " + info.getNm() + " (" + unit.getNm() + ")");
    }
    p.setPrdInfoSeq(info.getSeq());
    p.setUnitSeq(unit.getSeq());
    p.setUnitCnts(normalizeUnitCnts(unit, req.unitCnts()));
  }

  private Ingredient findOrCreateIngredient(String nm) {
    return ingredientRepo
        .findByNm(nm)
        .orElseGet(
            () -> {
              Ingredient i = new Ingredient();
              i.setNm(nm);
              OffsetDateTime now = OffsetDateTime.now();
              i.setRegAt(now);
              i.setModAt(now);
              return ingredientRepo.save(i);
            });
  }

  private ProductInfo findOrCreateInfo(Short ingdSeq, String nm) {
    return productInfoRepo
        .findByIngdSeqAndNm(ingdSeq, nm)
        .orElseGet(
            () -> {
              ProductInfo info = new ProductInfo();
              info.setIngdSeq(ingdSeq);
              info.setNm(nm);
              OffsetDateTime now = OffsetDateTime.now();
              info.setRegAt(now);
              info.setModAt(now);
              return productInfoRepo.save(info);
            });
  }

  private void removeInfoIfOrphan(Short infoSeq) {
    if (!productRepo.existsByPrdInfoSeq(infoSeq)) {
      productInfoRepo.deleteById(infoSeq);
    }
  }

  /** 단위수량 미사용 단위면 빈 배열, 사용 단위면 scale 2 통일(600 == 600.00) 후 중복 제거 + 오름차순. */
  private BigDecimal[] normalizeUnitCnts(Unit unit, List<BigDecimal> unitCnts) {
    if (!unit.getIsUnitCnt() || unitCnts == null) {
      return new BigDecimal[0];
    }
    return unitCnts.stream()
        .map(v -> v.setScale(2, RoundingMode.UNNECESSARY)) // @Digits(fraction = 2) 로 검증됨
        .distinct()
        .sorted()
        .toArray(BigDecimal[]::new);
  }

  private ProductRes toRes(Product p) {
    ProductInfo info = productInfoRepo.findById(p.getPrdInfoSeq()).orElseThrow();
    return toRes(
        p,
        info,
        bySeq(ingredientRepo.findAllById(List.of(info.getIngdSeq())), Ingredient::getSeq),
        bySeq(unitRepo.findAllById(List.of(p.getUnitSeq())), Unit::getSeq));
  }

  private ProductRes toRes(
      Product p, ProductInfo info, Map<Short, Ingredient> ingds, Map<Short, Unit> units) {
    BigDecimal[] cnts = p.getUnitCnts() != null ? p.getUnitCnts() : new BigDecimal[0];
    return new ProductRes(
        p.getSeq(),
        info.getIngdSeq(),
        ingds.get(info.getIngdSeq()).getNm(),
        info.getSeq(),
        info.getNm(),
        p.getUnitSeq(),
        units.get(p.getUnitSeq()).getNm(),
        Arrays.asList(cnts));
  }

  private static <T> Map<Short, T> bySeq(List<T> list, Function<T, Short> key) {
    return list.stream().collect(Collectors.toMap(key, Function.identity()));
  }
}
