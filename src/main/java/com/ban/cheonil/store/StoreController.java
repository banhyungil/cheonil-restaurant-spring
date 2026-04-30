package com.ban.cheonil.store;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.store.dto.StorePatchActiveReq;
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

  /** 활성 토글. */
  @PatchMapping("/{seq}/active")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void patchActive(@PathVariable Short seq, @RequestBody StorePatchActiveReq req) {
    storeService.patchActive(seq, req.active());
  }
}
