package com.ban.cheonil.menu;

import com.ban.cheonil.menu.dto.MenuCategoryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu-categories")
@RequiredArgsConstructor
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;

    @GetMapping
    public List<MenuCategoryRes> list() {
        return menuCategoryService.findAll();
    }
}
