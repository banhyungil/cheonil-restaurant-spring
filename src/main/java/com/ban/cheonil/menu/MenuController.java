package com.ban.cheonil.menu;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.menu.dto.MenuPatchActiveReq;
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

  /** 활성 토글. */
  @PatchMapping("/{seq}/active")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void patchActive(@PathVariable Short seq, @RequestBody MenuPatchActiveReq req) {
    menuService.patchActive(seq, req.active());
  }
}
