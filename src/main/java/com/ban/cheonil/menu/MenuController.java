package com.ban.cheonil.menu;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.menu.dto.MenuRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

  private final MenuService menuService;

  @GetMapping
  public List<MenuRes> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
    return menuService.findAll(includeInactive);
  }
}
