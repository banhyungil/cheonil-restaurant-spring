package com.ban.cheonil.store;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.store.dto.StoreCreateReq;
import com.ban.cheonil.store.dto.StoreRes;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

  private final StoreRepo storeRepo;

  /**
   * @param includeInactive true 면 비활성 매장도 포함 (관리자 페이지용). false 면 활성만 (영업/주문 페이지용).
   */
  public List<StoreRes> findAll(boolean includeInactive) {
    var stores = includeInactive ? storeRepo.findAll() : storeRepo.findByActiveTrue();
    return stores.stream().map(StoreRes::from).toList();
  }

  public StoreRes findBySeq(Short seq) {
    return StoreRes.from(get(seq));
  }

  @Transactional
  public StoreRes create(StoreCreateReq req) {
    Store s = new Store();
    apply(s, req);
    OffsetDateTime now = OffsetDateTime.now();
    s.setRegAt(now);
    s.setModAt(now);
    return StoreRes.from(storeRepo.save(s));
  }

  /** 전체 교체 (PUT). latitude/longitude/options 는 보존 (관리 UI 미노출). */
  @Transactional
  public StoreRes update(Short seq, StoreCreateReq req) {
    Store s = get(seq);
    apply(s, req);
    s.setModAt(OffsetDateTime.now());
    return StoreRes.from(s);
  }

  @Transactional
  public void remove(Short seq) {
    Store s = get(seq);
    storeRepo.delete(s);
  }

  /** 활성 토글 — PATCH /active. */
  @Transactional
  public void patchActive(Short seq, Boolean active) {
    Store s = get(seq);
    s.setActive(active);
    s.setModAt(OffsetDateTime.now());
  }

  /* ==================== helpers ==================== */

  private Store get(Short seq) {
    return storeRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("store " + seq + " not found"));
  }

  private void apply(Store s, StoreCreateReq req) {
    s.setCtgSeq(req.ctgSeq());
    s.setNm(req.nm());
    s.setAddr(req.addr());
    s.setCmt(req.cmt());
    s.setActive(req.active() != null ? req.active() : Boolean.TRUE);
  }
}
