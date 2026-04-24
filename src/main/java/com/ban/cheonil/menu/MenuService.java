package com.ban.cheonil.menu;

import com.ban.cheonil.menu.dto.MenuRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepo menuRepo;

    public List<MenuRes> findAll() {
        return menuRepo.findAll().stream().map(MenuRes::from).toList();
    }
}
