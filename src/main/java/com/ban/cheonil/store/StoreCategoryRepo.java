package com.ban.cheonil.store;

import com.ban.cheonil.store.entity.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCategoryRepo extends JpaRepository<StoreCategory, Short> {
}
