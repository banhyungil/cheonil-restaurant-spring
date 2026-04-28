package com.ban.cheonil.orderRsvTmpl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
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

import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplCreateReq;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplExtRes;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplMenuExtRes;
import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplsListParams;
import com.ban.cheonil.orderRsvTmpl.entity.DayType;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmplMenu;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmplMenuId;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderRsvTmplService {

  private final OrderRsvTmplRepo orderRsvTmplRepo;
  private final OrderRsvTmplMenuRepo orderRsvTmplMenuRepo;
  private final StoreRepo storeRepo;

  /* =========================================================
   * Create
   * ========================================================= */

  @Transactional
  public OrderRsvTmplExtRes create(OrderRsvTmplCreateReq req) {
    int amount = req.menus().stream().mapToInt(i -> i.price() * i.cnt()).sum();

    OrderRsvTmpl tmpl = new OrderRsvTmpl();
    tmpl.setStoreSeq(req.storeSeq());
    tmpl.setNm(req.nm());
    tmpl.setAmount(amount);
    tmpl.setRsvTime(req.rsvTime());
    tmpl.setDayTypes(toDayTypeArray(req.dayTypes()));
    tmpl.setStartDt(req.startDt() != null ? req.startDt() : LocalDate.now());
    tmpl.setEndDt(req.endDt());
    tmpl.setCmt(req.cmt());
    tmpl.setActive(req.active() != null ? req.active() : Boolean.TRUE);
    OffsetDateTime now = OffsetDateTime.now();
    tmpl.setRegAt(now);
    tmpl.setModAt(now);
    OrderRsvTmpl saved = orderRsvTmplRepo.save(tmpl);

    saveMenus(saved.getSeq(), req);

    return assemble(List.of(saved)).getFirst();
  }

  /* =========================================================
   * Read
   * ========================================================= */

  @Transactional(readOnly = true)
  public List<OrderRsvTmplExtRes> findByParams(OrderRsvTmplsListParams params) {
    List<OrderRsvTmpl> tmpls =
        orderRsvTmplRepo.findAll(buildSpec(params), Sort.by(Sort.Direction.ASC, "seq"));
    if (tmpls.isEmpty()) return List.of();
    return assemble(tmpls);
  }

  @Transactional(readOnly = true)
  public OrderRsvTmplExtRes findExtBySeq(Short seq) {
    OrderRsvTmpl tmpl =
        orderRsvTmplRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsvTmpl " + seq + " not found"));
    return assemble(List.of(tmpl)).getFirst();
  }

  /* =========================================================
   * Update
   * ========================================================= */

  /** 전체 교체 (PUT). 메뉴 항목 통째 재구성. */
  @Transactional
  public OrderRsvTmplExtRes update(Short seq, OrderRsvTmplCreateReq req) {
    OrderRsvTmpl tmpl =
        orderRsvTmplRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsvTmpl " + seq + " not found"));

    int amount = req.menus().stream().mapToInt(i -> i.price() * i.cnt()).sum();
    tmpl.setStoreSeq(req.storeSeq());
    tmpl.setNm(req.nm());
    tmpl.setAmount(amount);
    tmpl.setRsvTime(req.rsvTime());
    tmpl.setDayTypes(toDayTypeArray(req.dayTypes()));
    tmpl.setStartDt(req.startDt() != null ? req.startDt() : tmpl.getStartDt());
    tmpl.setEndDt(req.endDt());
    tmpl.setCmt(req.cmt());
    if (req.active() != null) tmpl.setActive(req.active());
    tmpl.setModAt(OffsetDateTime.now());

    orderRsvTmplMenuRepo.deleteByIdRsvTmplSeq(seq);
    saveMenus(seq, req);

    return assemble(List.of(tmpl)).getFirst();
  }

  /** 활성 토글 — PATCH /active. */
  @Transactional
  public void patchActive(Short seq, Boolean active) {
    OrderRsvTmpl tmpl =
        orderRsvTmplRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsvTmpl " + seq + " not found"));
    tmpl.setActive(active);
    tmpl.setModAt(OffsetDateTime.now());
  }

  /* =========================================================
   * Delete
   * ========================================================= */

  /** 템플릿 삭제. 연결된 t_order_rsv 인스턴스는 rsv_tmpl_seq=NULL 로 자동 처리 (DB FK ON DELETE SET NULL 가정). */
  @Transactional
  public void remove(Short seq) {
    OrderRsvTmpl tmpl =
        orderRsvTmplRepo
            .findById(seq)
            .orElseThrow(() -> new EntityNotFoundException("orderRsvTmpl " + seq + " not found"));
    orderRsvTmplMenuRepo.deleteByIdRsvTmplSeq(seq);
    orderRsvTmplRepo.delete(tmpl);
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  private void saveMenus(Short tmplSeq, OrderRsvTmplCreateReq req) {
    List<OrderRsvTmplMenu> menus =
        req.menus().stream()
            .map(
                i -> {
                  OrderRsvTmplMenuId id = new OrderRsvTmplMenuId();
                  id.setMenuSeq(i.menuSeq());
                  id.setRsvTmplSeq(tmplSeq);

                  OrderRsvTmplMenu m = new OrderRsvTmplMenu();
                  m.setId(id);
                  m.setPrice(i.price());
                  m.setCnt(i.cnt());
                  return m;
                })
          .toList();
    orderRsvTmplMenuRepo.saveAll(menus);
  }

  private List<OrderRsvTmplExtRes> assemble(List<OrderRsvTmpl> tmpls) {
    List<Short> tmplSeqs = tmpls.stream().map(OrderRsvTmpl::getSeq).toList();
    Set<Short> storeSeqs =
        tmpls.stream().map(OrderRsvTmpl::getStoreSeq).collect(Collectors.toSet());

    Map<Short, List<OrderRsvTmplMenuExtRes>> menusByTmpl =
        orderRsvTmplMenuRepo.findExtsByTmplSeqs(tmplSeqs).stream()
            .collect(Collectors.groupingBy(OrderRsvTmplMenuExtRes::rsvTmplSeq));

    Map<Short, Store> storeMap =
        storeRepo.findAllById(storeSeqs).stream()
            .collect(Collectors.toMap(Store::getSeq, Function.identity()));

    return tmpls.stream()
        .map(
            t ->
                toExtRes(
                    t,
                    storeMap.get(t.getStoreSeq()),
                    menusByTmpl.getOrDefault(t.getSeq(), List.of())))
        .toList();
  }

  private OrderRsvTmplExtRes toExtRes(
      OrderRsvTmpl t, Store store, List<OrderRsvTmplMenuExtRes> menus) {
    return new OrderRsvTmplExtRes(
        t.getSeq(),
        t.getStoreSeq(),
        t.getNm(),
        t.getAmount(),
        t.getRsvTime(),
        toDayTypes(t.getDayTypes()),
        t.getCmt(),
        t.getActive(),
        t.getStartDt(),
        t.getEndDt(),
        t.getRegAt(),
        t.getModAt(),
        store != null ? store.getNm() : null,
        menus);
  }

  private Specification<OrderRsvTmpl> buildSpec(OrderRsvTmplsListParams p) {
    Specification<OrderRsvTmpl> spec = (r, q, cb) -> cb.conjunction();
    if (p.storeSeq() != null) {
      spec = spec.and((r, q, cb) -> cb.equal(r.get("storeSeq"), p.storeSeq()));
    }
    if (p.active() != null) {
      spec = spec.and((r, q, cb) -> cb.equal(r.get("active"), p.active()));
    }
    if (p.dayType() != null) {
      // PostgreSQL array_position(arr, val) — 찾으면 위치(int), 없으면 NULL.
      // dayTypes 는 day_type enum 배열이지만 implicit cast 로 String 비교 가능.
      String dayName = p.dayType().name();
      spec =
          spec.and(
              (r, q, cb) ->
                  cb.isNotNull(
                      cb.function(
                          "array_position",
                          Integer.class,
                          r.get("dayTypes"),
                          cb.literal(dayName))));
    }
    return spec;
  }

  /** {@code List<DayType>} → DB 저장용 {@code String[]} (PG day_type[] 호환). */
  private String[] toDayTypeArray(List<DayType> dayTypes) {
    if (dayTypes == null) return new String[0];
    return dayTypes.stream().map(DayType::name).toArray(String[]::new);
  }

  /** DB 의 {@code String[]} → {@code List<DayType>} (DTO 응답용). */
  private List<DayType> toDayTypes(String[] arr) {
    if (arr == null) return List.of();
    return Arrays.stream(arr).map(DayType::valueOf).toList();
  }
}
