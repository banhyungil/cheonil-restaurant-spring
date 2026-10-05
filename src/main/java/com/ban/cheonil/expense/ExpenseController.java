package com.ban.cheonil.expense;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.expense.dto.ExpenseRes;
import com.ban.cheonil.expense.dto.ExpenseSaveReq;
import com.ban.cheonil.expense.dto.ExpensesParams;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

  private final ExpenseService expenseService;

  /** 기간 + 필터 — 전체 응답 (클라 페이징). */
  @GetMapping
  public List<ExpenseRes> list(@Valid @ModelAttribute ExpensesParams params) {
    return expenseService.findAll(params);
  }

  /** 같은 일자 + 같은 매장 기존 지출 — 없으면 204. 등록 폼의 "불러오기" 용. */
  @GetMapping("/lookup")
  public ResponseEntity<ExpenseRes> lookup(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam Short storeSeq) {
    return expenseService
        .lookup(date, storeSeq)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /** 카테고리에서 쓴 지출명 — 최근 사용순. 지출명 추천용. */
  @GetMapping("/names")
  public List<String> names(@RequestParam Integer ctgSeq) {
    return expenseService.recentNames(ctgSeq);
  }

  @GetMapping("/{seq}")
  public ExpenseRes get(@PathVariable Long seq) {
    return expenseService.findBySeq(seq);
  }

  @PostMapping
  public ResponseEntity<ExpenseRes> create(@Valid @RequestBody ExpenseSaveReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(req));
  }

  /** 전체 교체 (PUT). */
  @PutMapping("/{seq}")
  public ExpenseRes update(@PathVariable Long seq, @Valid @RequestBody ExpenseSaveReq req) {
    return expenseService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Long seq) {
    expenseService.remove(seq);
  }
}
