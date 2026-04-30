package com.ban.cheonil.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /stores 생성 + PUT /stores/{seq} 전체 교체 페이로드.
 *
 * <p>{@code active} null 이면 default true. 매장 관리 UI 가 노출하지 않는 컬럼 (latitude, longitude, options) 은 update
 * 시 보존됨.
 */
public record StoreCreateReq(
    @NotNull Short ctgSeq,
    @NotBlank @Size(max = 45) String nm,
    /** 구역 — 위치 정보 (예: '원예 6번지 · B동 3층'). */
    @Size(max = 200) String addr,
    @Size(max = 1000) String cmt,
    Boolean active) {}
