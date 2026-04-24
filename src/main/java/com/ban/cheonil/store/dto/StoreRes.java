package com.ban.cheonil.store.dto;

import com.ban.cheonil.store.entity.Store;

import java.time.OffsetDateTime;
import java.util.Map;

public record StoreRes(
        Short seq,
        Short ctgSeq,
        String nm,
        String addr,
        String cmt,
        Double latitude,
        Double longitude,
        Map<String, Object> options,
        OffsetDateTime regAt,
        OffsetDateTime modAt
) {
    public static StoreRes from(Store s) {
        return new StoreRes(
                s.getSeq(),
                s.getCtgSeq(),
                s.getNm(),
                s.getAddr(),
                s.getCmt(),
                s.getLatitude(),
                s.getLongitude(),
                s.getOptions(),
                s.getRegAt(),
                s.getModAt()
        );
    }
}
