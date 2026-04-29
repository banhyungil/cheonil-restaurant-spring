package com.ban.cheonil.orderRsv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ban.cheonil.orderRsv.entity.OrderRsv;

public interface OrderRsvRepo
    extends JpaRepository<OrderRsv, Long>, JpaSpecificationExecutor<OrderRsv> {

  /** 스케줄러 멱등성 체크 — 같은 (템플릿, 예약시각) 으로 이미 생성됐는지. */
  boolean existsByRsvTmplSeqAndRsvAt(Short rsvTmplSeq, java.time.OffsetDateTime rsvAt);
}
