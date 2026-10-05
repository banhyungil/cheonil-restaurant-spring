package com.ban.cheonil.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.product.entity.Ingredient;

public interface IngredientRepo extends JpaRepository<Ingredient, Short> {

  Optional<Ingredient> findByNm(String nm);

  boolean existsByNmAndSeqNot(String nm, Short seq);
}
