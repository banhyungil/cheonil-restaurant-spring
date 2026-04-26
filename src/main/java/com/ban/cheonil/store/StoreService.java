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

  public List<StoreRes> findAll() {
    return storeRepo.findAll().stream().map(StoreRes::from).toList();
  }
}
