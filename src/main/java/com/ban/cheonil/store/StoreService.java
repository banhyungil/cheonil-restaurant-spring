package com.ban.cheonil.store;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.store.dto.StoreRes;

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
}
