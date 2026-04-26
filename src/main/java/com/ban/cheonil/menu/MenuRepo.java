package com.ban.cheonil.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.menu.entity.Menu;

public interface MenuRepo extends JpaRepository<Menu, Short> {}
