package com.ban.cheonil.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 제품 응답 — 식자재 / 제품정보 / 단위 이름을 조인해서 내려준다 (제품 관리 목록, 지출 품목 선택 UI 용).
 *
 * @param unitCnts 자주 쓰는 규격 목록 (예: [500, 600]). 단위수량 미사용 단위면 빈 배열
 */
public record ProductRes(
    Integer seq,
    Short ingdSeq,
    String ingdNm,
    Short prdInfoSeq,
    String nm,
    Short unitSeq,
    String unitNm,
    List<BigDecimal> unitCnts) {}
