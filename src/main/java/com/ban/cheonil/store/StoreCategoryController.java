package com.ban.cheonil.store;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.store.dto.StoreCategoryRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/store-categories")
@RequiredArgsConstructor
public class StoreCategoryController {

  private final StoreCategoryService storeCategoryService;

  @GetMapping
  public List<StoreCategoryRes> list() {
    return storeCategoryService.findAll();
  }
}
