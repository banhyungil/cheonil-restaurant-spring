package com.ban.cheonil.expense;

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

import com.ban.cheonil.expense.dto.ExpenseCategoryRes;
import com.ban.cheonil.expense.dto.ExpenseCategorySaveReq;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

  private final ExpenseCategoryService expenseCategoryService;

  /** flat 리스트 (path 순). */
  @GetMapping
  public List<ExpenseCategoryRes> list() {
    return expenseCategoryService.findAll();
  }

  @PostMapping
  public ResponseEntity<ExpenseCategoryRes> create(@Valid @RequestBody ExpenseCategorySaveReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(expenseCategoryService.create(req));
  }

  /** 전체 교체 (PUT) — parentSeq 가 바뀌면 하위 트리째 이동. */
  @PutMapping("/{seq}")
  public ExpenseCategoryRes update(
      @PathVariable Integer seq, @Valid @RequestBody ExpenseCategorySaveReq req) {
    return expenseCategoryService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Integer seq) {
    expenseCategoryService.remove(seq);
  }
}
