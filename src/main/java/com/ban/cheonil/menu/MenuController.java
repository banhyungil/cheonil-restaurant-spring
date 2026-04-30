package com.ban.cheonil.menu;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.menu.dto.MenuCreateReq;
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

  @GetMapping("/{seq}")
  public MenuRes get(@PathVariable Short seq) {
    return menuService.findBySeq(seq);
  }

  @PostMapping
  public ResponseEntity<MenuRes> create(@Valid @RequestBody MenuCreateReq req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(req));
  }

  /** 전체 교체 (PUT). */
  @PutMapping("/{seq}")
  public MenuRes update(@PathVariable Short seq, @Valid @RequestBody MenuCreateReq req) {
    return menuService.update(seq, req);
  }

  @DeleteMapping("/{seq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Short seq) {
    menuService.remove(seq);
  }

  /** 활성 토글. */
  @PatchMapping("/{seq}/active")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void patchActive(@PathVariable Short seq, @RequestBody MenuPatchActiveReq req) {
    menuService.patchActive(seq, req.active());
  }
}
