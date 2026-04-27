package com.ban.cheonil.orderRsvTmpl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;

public interface OrderRsvTmplRepo
    extends JpaRepository<OrderRsvTmpl, Short>, JpaSpecificationExecutor<OrderRsvTmpl> {}
