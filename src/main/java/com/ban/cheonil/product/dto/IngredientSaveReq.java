package com.ban.cheonil.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /ingredients 생성 + PUT /ingredients/{seq} 이름 변경 페이로드. */
public record IngredientSaveReq(@NotBlank @Size(max = 100) String nm) {}
