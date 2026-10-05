package com.ban.cheonil.product;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.product.dto.UnitRes;
import com.ban.cheonil.product.dto.UnitSaveReq;
import com.ban.cheonil.product.entity.Unit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitService {

  private final UnitRepo unitRepo;
  private final ProductRepo productRepo;

  public List<UnitRes> findAll() {
    return unitRepo.findAll(Sort.by("seq")).stream().map(UnitRes::from).toList();
  }

  @Transactional
  public UnitRes create(UnitSaveReq req) {
    if (unitRepo.existsByNm(req.nm())) {
      throw new IllegalStateException("이미 존재하는 단위입니다: " + req.nm());
    }
    Unit u = new Unit();
    apply(u, req);
    return UnitRes.from(unitRepo.save(u));
  }

  /** 전체 교체 (PUT). */
  @Transactional
  public UnitRes update(Short seq, UnitSaveReq req) {
    Unit u = get(seq);
    if (unitRepo.existsByNmAndSeqNot(req.nm(), seq)) {
      throw new IllegalStateException("이미 존재하는 단위입니다: " + req.nm());
    }
    if (req.baseUnitSeq() != null && unitRepo.existsByBaseUnitSeq(seq)) {
      throw new IllegalStateException("다른 단위의 기준 단위라 기준 단위를 지정할 수 없습니다.");
    }
    if (u.getIsUnitCnt() && !req.isUnitCnt()) {
      long cnt = productRepo.countByUnitSeqWithUnitCnts(seq);
      if (cnt > 0) {
        throw new IllegalStateException("단위수량을 사용하는 제품이 " + cnt + "개 있어 해제할 수 없습니다.");
      }
    }
    apply(u, req);
    return UnitRes.from(u);
  }

  @Transactional
  public void remove(Short seq) {
    Unit u = get(seq);
    long cnt = productRepo.countByUnitSeq(seq);
    if (cnt > 0) {
      throw new IllegalStateException(cnt + "개 제품에서 사용 중인 단위입니다.");
    }
    if (unitRepo.existsByBaseUnitSeq(seq)) {
      throw new IllegalStateException("다른 단위의 기준 단위라 삭제할 수 없습니다.");
    }
    unitRepo.delete(u);
  }

  /* ==================== helpers ==================== */

  private Unit get(Short seq) {
    return unitRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("unit " + seq + " not found"));
  }

  /** 기준 단위는 자기 자신이 기준 단위(base 없음)인 단위만 가능 — g → kg 는 허용, g → mg → kg 같은 연쇄는 불가. */
  private void apply(Unit u, UnitSaveReq req) {
    u.setNm(req.nm());
    u.setIsUnitCnt(req.isUnitCnt());

    if (req.baseUnitSeq() == null) {
      u.setBaseUnitSeq(null);
      u.setBaseFactor(null);
      return;
    }
    if (req.baseUnitSeq().equals(u.getSeq())) {
      throw new IllegalArgumentException("자기 자신을 기준 단위로 지정할 수 없습니다.");
    }
    if (req.baseFactor() == null) {
      throw new IllegalArgumentException("기준 단위를 지정하면 환산계수가 필요합니다.");
    }
    Unit base = get(req.baseUnitSeq());
    if (base.getBaseUnitSeq() != null) {
      throw new IllegalArgumentException("기준 단위로 지정할 수 없는 단위입니다: " + base.getNm());
    }
    u.setBaseUnitSeq(base.getSeq());
    u.setBaseFactor(req.baseFactor());
  }
}
