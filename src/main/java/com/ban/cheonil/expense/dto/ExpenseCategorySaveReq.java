package com.ban.cheonil.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /expense-categories 생성 + PUT /expense-categories/{seq} 전체 교체 페이로드.
 *
 * <p>PUT 에서 parentSeq 가 바뀌면 하위 트리째 이동한다.
 *
 * @param parentSeq 상위 카테고리. null 이면 최상위
 */
public record ExpenseCategorySaveReq(Integer parentSeq, @NotBlank @Size(max = 50) String nm) {}
