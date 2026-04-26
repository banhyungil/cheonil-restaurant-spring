package com.ban.cheonil.store;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.store.dto.StoreCategoryRes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreCategoryService {

  private final StoreCategoryRepo storeCategoryRepo;

  public List<StoreCategoryRes> findAll() {
    return storeCategoryRepo.findAll().stream().map(StoreCategoryRes::from).toList();
  }
}
