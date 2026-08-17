package com.ban.cheonil.tts.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** DELETE /tts/cache 페이로드 — 관리 화면에서 체크한 항목 다중 삭제. key 는 SHA-256 hex. */
public record TtsCacheDeleteReq(@NotEmpty List<String> keys) {}
