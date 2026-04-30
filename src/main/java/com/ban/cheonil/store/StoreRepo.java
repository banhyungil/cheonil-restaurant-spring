package com.ban.cheonil.store;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.store.entity.Store;

public interface StoreRepo extends JpaRepository<Store, Short> {

  /** 활성 매장만 — 영업/주문 페이지용. */
  List<Store> findByActiveTrue();
}
