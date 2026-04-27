package com.ban.cheonil.orderRsvTmpl;

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

import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplCreateReq;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplExtRes;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplPatchActiveReq;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplsListParams;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order-rsv-tmpls")
@RequiredArgsConstructor
public class OrderRsvTmplController {

  private final OrderRsvTmplService orderRsvTmplService;

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
  public void patchActive(
      @PathVariable Short seq, @RequestBody OrderRsvTmplPatchActiveReq req) {
    orderRsvTmplService.patchActive(seq, req.active());
  }

  /** 템플릿 삭제. 연결된 인스턴스는 DB 가 rsv_tmpl_seq=NULL 처리 (FK ON DELETE SET NULL 가정). */
  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Short seq) {
    orderRsvTmplService.remove(seq);
  }
}
