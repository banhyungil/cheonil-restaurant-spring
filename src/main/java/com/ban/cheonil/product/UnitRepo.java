package com.ban.cheonil.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.product.entity.Unit;

public interface UnitRepo extends JpaRepository<Unit, Short> {

  boolean existsByNm(String nm);

  boolean existsByNmAndSeqNot(String nm, Short seq);

  /** 다른 단위의 기준 단위로 쓰이는지. */
  boolean existsByBaseUnitSeq(Short baseUnitSeq);
}
