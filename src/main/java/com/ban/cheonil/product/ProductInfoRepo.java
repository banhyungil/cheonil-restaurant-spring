package com.ban.cheonil.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.product.entity.ProductInfo;

public interface ProductInfoRepo extends JpaRepository<ProductInfo, Short> {

  Optional<ProductInfo> findByIngdSeqAndNm(Short ingdSeq, String nm);

  boolean existsByIngdSeq(Short ingdSeq);
}
