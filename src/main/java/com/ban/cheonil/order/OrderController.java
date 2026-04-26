package com.ban.cheonil.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.order.dto.OrderCreateReq;
import com.ban.cheonil.order.dto.OrderExtRes;
import com.ban.cheonil.order.dto.OrderRes;
import com.ban.cheonil.order.dto.OrderStatusChangeReq;
import com.ban.cheonil.order.dto.OrdersListParams;

import lombok.RequiredArgsConstructor;

/**
 * 직렬화 역직렬화 과정, Spring MVC + Jackson
 *
 * <p>HTTP 요청 (JSON) ↓ Spring MVC: @RestController + @RequestBody 인식 ↓ HttpMessageConverter (기본:
 * MappingJackson2HttpMessageConverter) ↓ Jackson ObjectMapper.readValue(json, OrderCreateReq.class)
 * ↓ Record canonical constructor 호출 → OrderCreateReq 인스턴스
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  /**
   * 주문 목록 — 매장/메뉴 join aggregate. Query params: statuses, cookedSince, storeSeq, orderFrom, orderTo
   * (모두 optional). 배열 statuses 는 콤마 join 형식 (?statuses=READY,COOKED) 으로 받음.
   */
  @GetMapping
  public List<OrderExtRes> list(@ModelAttribute OrdersListParams params) {
    return orderService.findByParams(params);
  }

  /** 주문 단건 — 매장/메뉴 join aggregate. */
  // @GetMapping("/{seq}")
  // public OrderExtRes get(@PathVariable Long seq) {
  //     return orderService.findExtBySeq(seq);
  // }

  /** 주문 상태 전이. cookedAt 은 서버가 자동 처리. */
  @PatchMapping("/{seq}/status")
  public OrderExtRes changeStatus(@PathVariable Long seq, @RequestBody OrderStatusChangeReq req) {
    return orderService.changeStatus(seq, req.status());
  }

  @PostMapping
  public ResponseEntity<OrderRes> create(@RequestBody OrderCreateReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(req));
  }
}
