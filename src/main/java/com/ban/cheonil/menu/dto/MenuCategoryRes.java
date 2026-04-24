package com.ban.cheonil.menu.dto;

import com.ban.cheonil.menu.entity.MenuCategory;

import java.time.OffsetDateTime;
import java.util.Map;

public record MenuCategoryRes(
        Short seq,
        String nm,
        Map<String, Object> options,
        OffsetDateTime regAt,
        OffsetDateTime modAt
) {
    public static MenuCategoryRes from(MenuCategory c) {
        return new MenuCategoryRes(
                c.getSeq(),
                c.getNm(),
                c.getOptions(),
                c.getRegAt(),
                c.getModAt()
        );
    }
}
