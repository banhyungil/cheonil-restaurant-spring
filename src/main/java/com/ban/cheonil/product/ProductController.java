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

import com.ban.cheonil.product.dto.ProductRes;
import com.ban.cheonil.product.dto.ProductSaveReq;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  /** 전체 제품 (식자재 / 단위 이름 조인). */
  @GetMapping
  public List<ProductRes> list() {
    return productService.findAll();
  }

  @PostMapping
  public ResponseEntity<ProductRes> create(@Valid @RequestBody ProductSaveReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
  }

  /** 전체 교체 (PUT). */
  @PutMapping("/{seq}")
  public ProductRes update(@PathVariable Integer seq, @Valid @RequestBody ProductSaveReq req) {
    return productService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Integer seq) {
    productService.remove(seq);
  }
}
