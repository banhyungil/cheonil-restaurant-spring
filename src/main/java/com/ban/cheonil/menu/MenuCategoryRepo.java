package com.ban.cheonil.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.menu.entity.MenuCategory;

public interface MenuCategoryRepo extends JpaRepository<MenuCategory, Short> {}
