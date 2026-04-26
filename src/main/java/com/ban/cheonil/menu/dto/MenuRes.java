package com.ban.cheonil.menu.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import com.ban.cheonil.menu.entity.Menu;

public record MenuRes(
    Short seq,
    Short ctgSeq,
    String nm,
    String nmS,
    Integer price,
    String cmt,
    Map<String, Object> options,
    OffsetDateTime regAt,
    OffsetDateTime modAt) {
  public static MenuRes from(Menu m) {
    return new MenuRes(
        m.getSeq(),
        m.getCtgSeq(),
        m.getNm(),
        m.getNmS(),
        m.getPrice(),
        m.getCmt(),
        m.getOptions(),
        m.getRegAt(),
        m.getModAt());
  }
}
