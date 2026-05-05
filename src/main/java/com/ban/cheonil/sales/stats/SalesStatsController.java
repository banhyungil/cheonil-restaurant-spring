package com.ban.cheonil.sales.stats;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.sales.stats.dto.DateRangeParams;
import com.ban.cheonil.sales.stats.dto.StatsBasicRes;
import com.ban.cheonil.sales.stats.dto.StatsMenuRes;
import com.ban.cheonil.sales.stats.dto.StatsStoreRes;
import com.ban.cheonil.sales.stats.dto.StatsTrendParams;
import com.ban.cheonil.sales.stats.dto.StatsTrendRes;

import lombok.RequiredArgsConstructor;

/** 주문내역관리 - 통계 탭 4 sub-view 엔드포인트. */
@RestController
@RequestMapping("/sales/stats")
@RequiredArgsConstructor
public class SalesStatsController {

  private final SalesStatsService salesStatsService;

  /** 통계 - 기본 뷰 (시간대 / 점포 TOP 5 / 결제유형 / 메뉴 TOP 5 + 헤더 KPI). */
  @GetMapping("/basic")
  public StatsBasicRes basic(@Valid @ModelAttribute DateRangeParams params) {
    return salesStatsService.basic(params);
  }

  /** 매출 추이 — 차트 로컬 segment 변경 시만 호출. */
  @GetMapping("/trend")
  public StatsTrendRes trend(@Valid @ModelAttribute StatsTrendParams params) {
    return salesStatsService.trend(params);
  }

  /** 통계 - 메뉴 분석 뷰. */
  @GetMapping("/menu")
  public StatsMenuRes menu(@Valid @ModelAttribute DateRangeParams params) {
    return salesStatsService.menu(params);
  }

  /** 통계 - 점포 분석 뷰. 모든 점포 데이터 반환 — frontend 가 multi-select 로 표시 매장 선택. */
  @GetMapping("/store")
  public StatsStoreRes store(@Valid @ModelAttribute DateRangeParams params) {
    return salesStatsService.store(params);
  }
}
