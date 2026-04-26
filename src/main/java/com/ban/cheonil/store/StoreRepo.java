package com.ban.cheonil.store;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.store.entity.Store;

public interface StoreRepo extends JpaRepository<Store, Short> {}
