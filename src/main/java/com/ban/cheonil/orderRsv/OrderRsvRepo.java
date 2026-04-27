package com.ban.cheonil.orderRsv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ban.cheonil.orderRsv.entity.OrderRsv;

public interface OrderRsvRepo
    extends JpaRepository<OrderRsv, Long>, JpaSpecificationExecutor<OrderRsv> {}
