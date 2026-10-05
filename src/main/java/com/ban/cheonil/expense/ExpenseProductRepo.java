package com.ban.cheonil.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.expense.entity.ExpenseProduct;

/** 지출 제품 상세 Repository. */
public interface ExpenseProductRepo extends JpaRepository<ExpenseProduct, Long> {}
