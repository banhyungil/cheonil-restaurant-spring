package com.ban.cheonil.menu;

import com.ban.cheonil.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepo extends JpaRepository<Menu, Short> {
}
