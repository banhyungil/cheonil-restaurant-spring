package com.ban.cheonil.store.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import com.ban.cheonil.store.entity.Store;

public record StoreRes(
    Short seq,
    Short ctgSeq,
    String nm,
    String addr,
    String cmt,
    Double latitude,
    Double longitude,
    Boolean active,
    Map<String, Object> options,
    OffsetDateTime regAt,
    OffsetDateTime modAt) {
  public static StoreRes from(Store s) {
    return new StoreRes(
        s.getSeq(),
        s.getCtgSeq(),
        s.getNm(),
        s.getAddr(),
        s.getCmt(),
        s.getLatitude(),
        s.getLongitude(),
        s.getActive(),
        s.getOptions(),
        s.getRegAt(),
        s.getModAt());
  }
}
