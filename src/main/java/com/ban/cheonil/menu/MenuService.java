package com.ban.cheonil.menu;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.menu.dto.MenuRes;

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
}
