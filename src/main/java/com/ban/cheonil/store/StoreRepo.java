package com.ban.cheonil.store;

import com.ban.cheonil.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepo extends JpaRepository<Store, Short> {
}
