package com.ban.cheonil.product;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.product.dto.UnitRes;
import com.ban.cheonil.product.dto.UnitSaveReq;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
public class UnitController {

  private final UnitService unitService;

  @GetMapping
  public List<UnitRes> list() {
    return unitService.findAll();
  }

  @PostMapping
  public ResponseEntity<UnitRes> create(@Valid @RequestBody UnitSaveReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(unitService.create(req));
  }

  /** 전체 교체 (PUT). */
  @PutMapping("/{seq}")
  public UnitRes update(@PathVariable Short seq, @Valid @RequestBody UnitSaveReq req) {
    return unitService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Short seq) {
    unitService.remove(seq);
  }
}
