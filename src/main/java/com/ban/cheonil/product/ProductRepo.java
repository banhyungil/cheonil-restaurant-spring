package com.ban.cheonil.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.product.entity.Product;

public interface ProductRepo extends JpaRepository<Product, Integer> {

  long countByUnitSeq(Short unitSeq);

  /** 해당 단위를 쓰면서 규격(unit_cnts) 값이 있는 제품 수. */
  @Query(
      value =
          "select count(*) from m_product where unit_seq = :unitSeq and cardinality(unit_cnts) > 0",
      nativeQuery = true)
  long countByUnitSeqWithUnitCnts(@Param("unitSeq") Short unitSeq);
}
