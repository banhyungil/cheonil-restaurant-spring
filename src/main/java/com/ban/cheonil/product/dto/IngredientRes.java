package com.ban.cheonil.product.dto;

/** 식자재 응답. productCnt 는 이 식자재에 속한 제품(제품정보 + 단위) 수. */
public record IngredientRes(Short seq, String nm, long productCnt) {}
