package com.ban.cheonil.orderRsv;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.orderRsv.dto.OrderRsvCreateReq;
import com.ban.cheonil.orderRsv.dto.OrderRsvExtRes;
import com.ban.cheonil.orderRsv.dto.OrderRsvMenuExtRes;
import com.ban.cheonil.orderRsv.dto.OrderRsvStatusChangeRes;
import com.ban.cheonil.orderRsv.dto.OrderRsvsListParams;
import com.ban.cheonil.orderRsv.dto.OrderRsvsListParams.DayMode;
import com.ban.cheonil.orderRsv.entity.OrderRsv;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenu;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenuId;
import com.ban.cheonil.orderRsv.entity.RsvStatus;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderRsvService {

  private final OrderRsvRepo orderRsvRepo;
  private final OrderRsvMenuRepo orderRsvMenuRepo;
  private final StoreRepo storeRepo;

  /* =========================================================
   * Create
   * ========================================================= */

  @Transactional
  public OrderRsvExtRes create(OrderRsvCreateReq req) {
    int amount = req.menus().stream().mapToInt(i -> i.price() * i.cnt()).sum();

    OrderRsv rsv = new OrderRsv();
    rsv.setStoreSeq(req.storeSeq());
    rsv.setRsvAt(req.rsvAt());
    rsv.setAmount(amount);
    rsv.setStatus(RsvStatus.RESERVED);
    rsv.setCmt(req.cmt());
    OffsetDateTime now = OffsetDateTime.now();
    rsv.setRegAt(now);
    rsv.setModAt(now);
    OrderRsv saved = orderRsvRepo.save(rsv);

    List<OrderRsvMenu> menus =
        req.menus().stream()
            .map(
                i -> {
                  OrderRsvMenuId id = new OrderRsvMenuId();
                  id.setMenuSeq(i.menuSeq());
                  id.setRsvSeq(saved.getSeq());

                  OrderRsvMenu m = new OrderRsvMenu();
                  m.setId(id);
                  m.setPrice(i.price());
                  m.setCnt(i.cnt());
                  return m;
                })
            .toList();
    orderRsvMenuRepo.saveAll(menus);

    return assemble(List.of(saved)).getFirst();
  }

  /* =========================================================
   * Read
   * ========================================================= */

  @Transactional(readOnly = true)
  public List<OrderRsvExtRes> findByParams(OrderRsvsListParams params) {
    List<OrderRsv> rsvs =
        orderRsvRepo.findAll(buildSpec(params), Sort.by(Sort.Direction.ASC, "rsvAt"));
    if (rsvs.isEmpty()) return List.of();
    return assemble(rsvs);
  }

  /* =========================================================
   * Update
   * ========================================================= */

  /** 전체 교체 (PUT). 메뉴 항목 통째 재구성. 상태 무관 — 모든 상태에서 수정 가능. */
  @Transactional
  public OrderRsvExtRes update(Long seq, OrderRsvCreateReq req) {
    OrderRsv rsv =
        orderRsvRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsv " + seq + " not found"));

    int amount = req.menus().stream().mapToInt(i -> i.price() * i.cnt()).sum();
    rsv.setStoreSeq(req.storeSeq());
    rsv.setRsvAt(req.rsvAt());
    rsv.setAmount(amount);
    rsv.setCmt(req.cmt());
    rsv.setModAt(OffsetDateTime.now());

    orderRsvMenuRepo.deleteByIdRsvSeq(seq);
    List<OrderRsvMenu> newMenus =
        req.menus().stream()
            .map(
                i -> {
                  OrderRsvMenuId id = new OrderRsvMenuId();
                  id.setMenuSeq(i.menuSeq());
                  id.setRsvSeq(seq);

                  OrderRsvMenu m = new OrderRsvMenu();
                  m.setId(id);
                  m.setPrice(i.price());
                  m.setCnt(i.cnt());
                  return m;
                })
            .toList();
    orderRsvMenuRepo.saveAll(newMenus);

    return assemble(List.of(rsv)).getFirst();
  }

  /**
   * 상태 전이. 모든 전이 허용 (프론트가 시간 기반 revertibility UI 제어).
   *
   * <p>TODO: RESERVED → COMPLETED 시 t_order 자동 INSERT (예약 → 실제 주문 변환). 현재는 status 만 변경.
   */
  @Transactional
  public OrderRsvStatusChangeRes changeStatus(Long seq, RsvStatus newStatus) {
    OrderRsv rsv =
        orderRsvRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsv " + seq + " not found"));

    rsv.setStatus(newStatus);
    rsv.setModAt(OffsetDateTime.now());

    return new OrderRsvStatusChangeRes(rsv.getSeq(), rsv.getStatus(), rsv.getModAt());
  }

  /* =========================================================
   * Delete
   * ========================================================= */

  @Transactional
  public void remove(Long seq) {
    OrderRsv rsv =
        orderRsvRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsv " + seq + " not found"));
    orderRsvMenuRepo.deleteByIdRsvSeq(seq);
    orderRsvRepo.delete(rsv);
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  /** rsv 리스트에 매장/메뉴 정보 batch fetch + Java 측 조립. 총 3 query. */
  private List<OrderRsvExtRes> assemble(List<OrderRsv> rsvs) {
    List<Long> rsvSeqs = rsvs.stream().map(OrderRsv::getSeq).toList();
    Set<Short> storeSeqs = rsvs.stream().map(OrderRsv::getStoreSeq).collect(Collectors.toSet());

    Map<Long, List<OrderRsvMenuExtRes>> menusByRsv =
        orderRsvMenuRepo.findExtsByRsvSeqs(rsvSeqs).stream()
            .collect(Collectors.groupingBy(OrderRsvMenuExtRes::rsvSeq));

    Map<Short, Store> storeMap =
        storeRepo.findAllById(storeSeqs).stream()
            .collect(Collectors.toMap(Store::getSeq, Function.identity()));

    return rsvs.stream()
        .map(
            r ->
                toExtRes(
                    r, storeMap.get(r.getStoreSeq()), menusByRsv.getOrDefault(r.getSeq(), List.of())))
        .toList();
  }

  private OrderRsvExtRes toExtRes(OrderRsv r, Store store, List<OrderRsvMenuExtRes> menus) {
    return new OrderRsvExtRes(
        r.getSeq(),
        r.getStoreSeq(),
        r.getRsvTmplSeq(),
        r.getAmount(),
        r.getRsvAt(),
        r.getStatus(),
        r.getCmt(),
        r.getRegAt(),
        r.getModAt(),
        store != null ? store.getNm() : null,
        store != null ? store.getCmt() : null,
        null, // tmplNm — m_order_rsv_tmpl join 미구현 (추후)
        menus);
  }

  private Specification<OrderRsv> buildSpec(OrderRsvsListParams p) {
    Specification<OrderRsv> spec = (r, q, cb) -> cb.conjunction();
    if (p.statuses() != null && !p.statuses().isEmpty()) {
      spec = spec.and((r, q, cb) -> r.get("status").in(p.statuses()));
    }
    if (p.storeSeq() != null) {
      spec = spec.and((r, q, cb) -> cb.equal(r.get("storeSeq"), p.storeSeq()));
    }
    if (p.dayMode() == DayMode.TODAY) {
      // 서버 기준 오늘 0시 ~ 내일 0시
      LocalDate today = LocalDate.now();
      OffsetDateTime start = today.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
      OffsetDateTime end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
      Specification<OrderRsv> finalSpec = spec;
      spec =
          finalSpec.and(
              (r, q, cb) ->
                  cb.and(
                      cb.greaterThanOrEqualTo(r.get("rsvAt"), start),
                      cb.lessThan(r.get("rsvAt"), end)));
    }
    return spec;
  }
}
