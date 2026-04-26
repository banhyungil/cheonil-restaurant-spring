package com.ban.cheonil.store.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import com.ban.cheonil.store.entity.StoreCategory;

public record StoreCategoryRes(
    Short seq, String nm, Map<String, Object> options, OffsetDateTime regAt, OffsetDateTime modAt) {
  public static StoreCategoryRes from(StoreCategory c) {
    return new StoreCategoryRes(c.getSeq(), c.getNm(), c.getOptions(), c.getRegAt(), c.getModAt());
  }
}
