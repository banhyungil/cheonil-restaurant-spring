package com.ban.cheonil.menu;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.menu.dto.MenuCreateReq;
import com.ban.cheonil.menu.dto.MenuRes;
import com.ban.cheonil.menu.entity.Menu;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

  private final MenuRepo menuRepo;

  /**
   * @param includeInactive true 면 비활성 메뉴도 포함 (관리자 페이지용). false 면 활성만 (영업/주문 페이지용).
   */
  public List<MenuRes> findAll(boolean includeInactive) {
    var menus = includeInactive ? menuRepo.findAll() : menuRepo.findByActiveTrue();
    return menus.stream().map(MenuRes::from).toList();
  }

  public MenuRes findBySeq(Short seq) {
    return MenuRes.from(get(seq));
  }

  @Transactional
  public MenuRes create(MenuCreateReq req) {
    Menu m = new Menu();
    apply(m, req);
    OffsetDateTime now = OffsetDateTime.now();
    m.setRegAt(now);
    m.setModAt(now);
    return MenuRes.from(menuRepo.save(m));
  }

  /** 전체 교체 (PUT). */
  @Transactional
  public MenuRes update(Short seq, MenuCreateReq req) {
    Menu m = get(seq);
    apply(m, req);
    m.setModAt(OffsetDateTime.now());
    return MenuRes.from(m);
  }

  @Transactional
  public void remove(Short seq) {
    Menu m = get(seq);
    menuRepo.delete(m);
  }

  /** 활성 토글 — PATCH /active. */
  @Transactional
  public void patchActive(Short seq, Boolean active) {
    Menu m = get(seq);
    m.setActive(active);
    m.setModAt(OffsetDateTime.now());
  }

  /* ==================== helpers ==================== */

  private Menu get(Short seq) {
    return menuRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("menu " + seq + " not found"));
  }

  private void apply(Menu m, MenuCreateReq req) {
    m.setCtgSeq(req.ctgSeq());
    m.setNm(req.nm());
    m.setNmS(req.nmS());
    m.setPrice(req.price());
    m.setCmt(req.cmt());
    m.setActive(req.active() != null ? req.active() : Boolean.TRUE);
  }
}
