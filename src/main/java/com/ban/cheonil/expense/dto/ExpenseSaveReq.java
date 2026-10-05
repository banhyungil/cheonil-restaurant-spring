package com.ban.cheonil.expense.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * POST /expenses 생성 + PUT /expenses/{seq} 전체 교체 페이로드.
 *
 * <p>같은 일자 + 같은 매장 지출은 하나만 — 이미 있으면 400. 프론트는 lookup 으로 기존 지출을 불러와 수정한다.
 *
 * @param expenseDt 지출일자 (KST). KST 자정 시각으로 저장
 * @param storeSeq 구입처 매장. 매장 없는 지출(공과금 등)은 null
 * @param products 구입목록 — PUT 에서는 전체 교체. null 이면 품목 없음
 */
public record ExpenseSaveReq(
    @NotNull Integer ctgSeq,
    Short storeSeq,
    @NotBlank @Size(max = 50) String nm,
    @NotNull @Positive Integer amount,
    @NotNull LocalDate expenseDt,
    @Size(max = 400) String cmt,
    List<@Valid @NotNull ExpenseProductReq> products) {}
