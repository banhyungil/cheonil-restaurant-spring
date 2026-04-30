package com.ban.cheonil.store;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.store.dto.StoreCreateReq;
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

  @GetMapping("/{seq}")
  public StoreRes get(@PathVariable Short seq) {
    return storeService.findBySeq(seq);
  }

  @PostMapping
  public ResponseEntity<StoreRes> create(@Valid @RequestBody StoreCreateReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(storeService.create(req));
  }

  /** 전체 교체 (PUT). */
  @PutMapping("/{seq}")
  public StoreRes update(@PathVariable Short seq, @Valid @RequestBody StoreCreateReq req) {
    return storeService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Short seq) {
    storeService.remove(seq);
  }

  /** 활성 토글. */
  @PatchMapping("/{seq}/active")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void patchActive(@PathVariable Short seq, @RequestBody StorePatchActiveReq req) {
    storeService.patchActive(seq, req.active());
  }
}
