package com.ban.cheonil.store;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  /** 활성 토글 — PATCH /active. */
  @Transactional
  public void patchActive(Short seq, Boolean active) {
    Store s =
        storeRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("store " + seq + " not found"));
    s.setActive(active);
    s.setModAt(OffsetDateTime.now());
  }
}
