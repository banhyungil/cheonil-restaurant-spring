package com.ban.cheonil.menu;

import com.ban.cheonil.menu.dto.MenuCategoryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuCategoryService {

    private final MenuCategoryRepo menuCategoryRepo;

    public List<MenuCategoryRes> findAll() {
        return menuCategoryRepo.findAll().stream().map(MenuCategoryRes::from).toList();
    }
}
