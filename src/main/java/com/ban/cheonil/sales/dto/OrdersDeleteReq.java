package com.ban.cheonil.sales.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** DELETE /sales/orders 페이로드 — 회계 정정용 다중 삭제. */
public record OrdersDeleteReq(@NotEmpty List<Long> orderSeqs) {}
