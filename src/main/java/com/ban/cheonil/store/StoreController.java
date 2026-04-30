package com.ban.cheonil.store;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.store.dto.StoreRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @GetMapping
  public List<StoreRes> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
    return storeService.findAll(includeInactive);
  }
}
