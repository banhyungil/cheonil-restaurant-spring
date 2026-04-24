package com.ban.cheonil.menu;

import com.ban.cheonil.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepo extends JpaRepository<MenuCategory, Short> {
}
