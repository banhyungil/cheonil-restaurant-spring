package com.ban.cheonil.sales;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.sales.dto.SalesSummaryParams;
import com.ban.cheonil.sales.dto.SalesSummaryRes;
import com.ban.cheonil.sales.dto.TransactionRes;
import com.ban.cheonil.sales.dto.TransactionsParams;
import com.ban.cheonil.sales.dto.UnpaidParams;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

  private final SalesService salesService;

  /** 정산 KPI 5 카드 — 단일 날짜 + 전일 비교. */
  @GetMapping("/summary")
  public SalesSummaryRes summary(@Valid @ModelAttribute SalesSummaryParams params) {
    return salesService.summary(params);
  }

  /** 정산 탭 — 그날 거래 내역 (lazy pagination). */
  @GetMapping("/transactions")
  public Page<TransactionRes> transactions(@Valid @ModelAttribute TransactionsParams params) {
    return salesService.transactions(params);
  }

  /** 수금 탭 — 모든 미수 (날짜 무관, lazy pagination). */
  @GetMapping("/unpaid")
  public Page<TransactionRes> unpaid(@ModelAttribute UnpaidParams params) {
    return salesService.unpaid(params);
  }
}
