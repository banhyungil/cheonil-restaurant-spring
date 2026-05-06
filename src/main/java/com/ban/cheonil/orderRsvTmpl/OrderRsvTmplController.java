package com.ban.cheonil.orderRsvTmpl;

import com.ban.cheonil.orderRsv.dto.OrderRsvExtRes;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplCreateReq;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplExtRes;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplPatchActiveReq;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplPatchAutoOrderReq;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplsListParams;
import com.ban.cheonil.orderRsvTmpl.scheduler.OrderRsvSchedulerService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-rsv-tmpls")
@RequiredArgsConstructor
public class OrderRsvTmplController {

  private final OrderRsvTmplService orderRsvTmplService;
  private final OrderRsvSchedulerService orderRsvSchedulerService;

  /** 템플릿 목록 — 매장/메뉴 join aggregate. dayType 필터는 day_types 배열에 해당 요일 포함된 것만. */
  @GetMapping
  public List<OrderRsvTmplExtRes> list(@ModelAttribute OrderRsvTmplsListParams params) {
    return orderRsvTmplService.findByParams(params);
  }

  /** 템플릿 단건 — 편집 페이지 hydrate 용. */
  @GetMapping("/{seq}")
  public OrderRsvTmplExtRes get(@PathVariable Short seq) {
    return orderRsvTmplService.findExtBySeq(seq);
  }

  /** 템플릿 생성. 응답으로 join 된 aggregate 반환. */
  @PostMapping
  public ResponseEntity<OrderRsvTmplExtRes> create(@RequestBody OrderRsvTmplCreateReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderRsvTmplService.create(req));
  }

  /** 템플릿 전체 교체 (PUT). 메뉴 항목 통째 재구성. */
  @PutMapping("/{seq}")
  public OrderRsvTmplExtRes update(
      @PathVariable Short seq, @RequestBody OrderRsvTmplCreateReq req) {
    return orderRsvTmplService.update(seq, req);
  }

  /** 활성 토글. */
  @PatchMapping("/{seq}/active")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void patchActive(@PathVariable Short seq, @RequestBody OrderRsvTmplPatchActiveReq req) {
    orderRsvTmplService.patchActive(seq, req.active());
  }

  /** 자동 주문 토글 — true 면 스케줄러가 예약 생성 시 주문(t_order) 도 즉시 생성. */
  @PatchMapping("/{seq}/auto-order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void patchAutoOrder(
      @PathVariable Short seq, @RequestBody OrderRsvTmplPatchAutoOrderReq req) {
    orderRsvTmplService.patchAutoOrder(seq, req.autoOrder());
  }

  /**
   * 단건 수동 트리거 — 특정 템플릿으로 오늘 일자 예약 즉시 생성.
   *
   * <p>스케줄러 룰 (active / day_types / start_dt / end_dt) 검증 안함 — 운영자 수동 강제. 이미 같은 (tmpl, today+rsvTime)
   * 으로 예약 있으면 멱등 처리 (기존 결과 반환).
   */
  @PostMapping("/{seq}/generate-rsv")
  public ResponseEntity<OrderRsvExtRes> generateRsvToday(@PathVariable Short seq) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(orderRsvTmplService.generateRsvToday(seq));
  }

  /** 템플릿 삭제. 연결된 인스턴스는 DB 가 rsv_tmpl_seq=NULL 처리 (FK ON DELETE SET NULL 가정). */
  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Short seq) {
    orderRsvTmplService.remove(seq);
  }

  /**
   * 수동 trigger — 지정된 window 의 활성 템플릿으로 인스턴스 생성. 운영/디버깅 용도. 멱등성 덕에 여러 번 호출 안전.
   *
   * <p>예: {@code POST
   * /api/order-rsv-tmpls/generate-rsv?windowStart=2026-04-29T12:30:00%2B09:00&windowMinutes=10}
   */
  @PostMapping("/generate-rsv")
  public Map<String, Integer> generateRsv(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime windowStart,
      @RequestParam(defaultValue = "10") int windowMinutes) {
    int createdCnt =
        orderRsvSchedulerService.generateForWindow(
            windowStart, windowStart.plusMinutes(windowMinutes));
    return Map.of("createdCnt", createdCnt);
  }
}
