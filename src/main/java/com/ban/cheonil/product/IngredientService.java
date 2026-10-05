package com.ban.cheonil.product;

import java.text.Collator;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.product.dto.IngredientRes;
import com.ban.cheonil.product.dto.IngredientSaveReq;
import com.ban.cheonil.product.entity.Ingredient;
import com.ban.cheonil.product.entity.ProductInfo;

import lombok.RequiredArgsConstructor;

/** 식자재 — 같은 식자재의 제품 간 가격 비교 기준. 주로 제품 등록 시 자동 생성되고, 여기서는 이름 변경 / 정리를 한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

  private static final Collator KO = Collator.getInstance(Locale.KOREAN);

  private final IngredientRepo ingredientRepo;
  private final ProductInfoRepo productInfoRepo;
  private final ProductRepo productRepo;

  /** 전체 식자재 (이름순) + 제품 수. */
  public List<IngredientRes> findAll() {
    Map<Short, Short> infoToIngd =
        productInfoRepo.findAll().stream()
            .collect(Collectors.toMap(ProductInfo::getSeq, ProductInfo::getIngdSeq));
    Map<Short, Long> productCnt =
        productRepo.findAll().stream()
            .collect(
                Collectors.groupingBy(
                    p -> infoToIngd.get(p.getPrdInfoSeq()), Collectors.counting()));

    return ingredientRepo.findAll().stream()
        .map(i -> new IngredientRes(i.getSeq(), i.getNm(), productCnt.getOrDefault(i.getSeq(), 0L)))
        .sorted(Comparator.comparing(IngredientRes::nm, KO))
        .toList();
  }

  @Transactional
  public IngredientRes create(IngredientSaveReq req) {
    String nm = req.nm().trim();
    if (ingredientRepo.findByNm(nm).isPresent()) {
      throw new IllegalStateException("이미 존재하는 식자재입니다: " + nm);
    }
    Ingredient i = new Ingredient();
    i.setNm(nm);
    OffsetDateTime now = OffsetDateTime.now();
    i.setRegAt(now);
    i.setModAt(now);
    return new IngredientRes(ingredientRepo.save(i).getSeq(), nm, 0);
  }

  /** 이름 변경. */
  @Transactional
  public void update(Short seq, IngredientSaveReq req) {
    Ingredient i = get(seq);
    String nm = req.nm().trim();
    if (ingredientRepo.existsByNmAndSeqNot(nm, seq)) {
      throw new IllegalStateException("이미 존재하는 식자재입니다: " + nm);
    }
    i.setNm(nm);
    i.setModAt(OffsetDateTime.now());
  }

  @Transactional
  public void remove(Short seq) {
    Ingredient i = get(seq);
    if (productInfoRepo.existsByIngdSeq(seq)) {
      throw new IllegalStateException("제품이 등록된 식자재라 삭제할 수 없습니다.");
    }
    ingredientRepo.delete(i);
  }

  private Ingredient get(Short seq) {
    return ingredientRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("ingredient " + seq + " not found"));
  }
}
