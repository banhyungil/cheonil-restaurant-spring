package com.ban.cheonil.orderRsv;

import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.orderRsv.dto.OrderRsvCreateReq;
import com.ban.cheonil.orderRsv.dto.OrderRsvExtRes;
import com.ban.cheonil.orderRsv.dto.OrderRsvStatusChangeReq;
import com.ban.cheonil.orderRsv.dto.OrderRsvStatusChangeRes;
import com.ban.cheonil.orderRsv.dto.OrderRsvsListParams;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order-rsvs")
@RequiredArgsConstructor
public class OrderRsvController {

  private final OrderRsvService orderRsvService;

  /** 예약 목록 — 매장/메뉴 join aggregate. statuses 는 콤마 join (?statuses=RESERVED,COMPLETED). */
  @GetMapping
  public List<OrderRsvExtRes> list(@ModelAttribute OrderRsvsListParams params) {
    return orderRsvService.findByParams(params);
  }

  /** 예약 생성 (일회성). 응답으로 join 된 aggregate 반환. */
  @PostMapping
  public ResponseEntity<OrderRsvExtRes> create(@RequestBody OrderRsvCreateReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderRsvService.create(req));
  }

  /** 예약 전체 교체 (PUT). 메뉴 항목 통째 재구성. */
  @PutMapping("/{seq}")
  public OrderRsvExtRes update(@PathVariable Long seq, @RequestBody OrderRsvCreateReq req) {
    return orderRsvService.update(seq, req);
  }

  /** 상태 전이 — 변경된 핵심 필드만 응답. */
  @PatchMapping("/{seq}/status")
  public OrderRsvStatusChangeRes changeStatus(
      @PathVariable Long seq, @RequestBody OrderRsvStatusChangeReq req) {
    return orderRsvService.changeStatus(seq, req.status());
  }

  /** 예약 삭제. */
  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Long seq) {
    orderRsvService.remove(seq);
  }
}
