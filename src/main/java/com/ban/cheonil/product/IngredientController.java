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

import com.ban.cheonil.product.dto.IngredientRes;
import com.ban.cheonil.product.dto.IngredientSaveReq;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

  private final IngredientService ingredientService;

  /** 전체 식자재 (이름순) + 제품 수. */
  @GetMapping
  public List<IngredientRes> list() {
    return ingredientService.findAll();
  }

  @PostMapping
  public ResponseEntity<IngredientRes> create(@Valid @RequestBody IngredientSaveReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ingredientService.create(req));
  }

  /** 이름 변경. */
  @PutMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@PathVariable Short seq, @Valid @RequestBody IngredientSaveReq req) {
    ingredientService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Short seq) {
    ingredientService.remove(seq);
  }
}
