package com.ban.cheonil.menu;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.menu.entity.Menu;

public interface MenuRepo extends JpaRepository<Menu, Short> {

  /** 활성 메뉴만 — 영업/주문 페이지용. */
  List<Menu> findByActiveTrue();
}
