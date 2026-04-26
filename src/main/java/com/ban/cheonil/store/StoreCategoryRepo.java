package com.ban.cheonil.store;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.store.entity.StoreCategory;

public interface StoreCategoryRepo extends JpaRepository<StoreCategory, Short> {}
