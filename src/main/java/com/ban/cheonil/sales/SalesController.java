package com.ban.cheonil.sales;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.sales.dto.OrderRowRes;
import com.ban.cheonil.sales.dto.OrdersDeleteReq;
import com.ban.cheonil.sales.dto.OrdersParams;
import com.ban.cheonil.sales.dto.OrdersSummaryRes;
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

  /* ---- 정산 페이지 ---- */

  /** 정산 KPI 5 카드 — 단일 날짜 + 전일 비교. */
  @GetMapping("/summary")
  public SalesSummaryRes summary(@Valid @ModelAttribute SalesSummaryParams params) {
    return salesService.summary(params);
  }

  /** 정산 탭 — 그날 거래 내역 (전체 응답, 클라 페이징/필터). */
  @GetMapping("/transactions")
  public List<TransactionRes> transactions(@Valid @ModelAttribute TransactionsParams params) {
    return salesService.transactions(params);
  }

  /** 수금 탭 — 모든 미수 (날짜 무관, lazy pagination). */
  @GetMapping("/unpaid")
  public Page<TransactionRes> unpaid(@ModelAttribute UnpaidParams params) {
    return salesService.unpaid(params);
  }

  /* ---- 주문내역관리 (그리드 탭) ---- */

  /** 그리드 탭 거래 내역 — 클라 페이징 (전체 응답). */
  @GetMapping("/orders")
  public List<OrderRowRes> orders(@Valid @ModelAttribute OrdersParams params) {
    return salesService.findOrders(params);
  }

  /** 그리드 탭 KPI 4 카드. */
  @GetMapping("/orders/summary")
  public OrdersSummaryRes ordersSummary(@Valid @ModelAttribute OrdersParams params) {
    return salesService.ordersSummary(params);
  }

  /** 그리드 탭 다중 삭제 — 회계 정정. */
  @DeleteMapping("/orders")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeOrders(@Valid @RequestBody OrdersDeleteReq req) {
    salesService.removeOrders(req.orderSeqs());
  }
}
