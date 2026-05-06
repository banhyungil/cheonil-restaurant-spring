package com.ban.cheonil.orderRsv;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ban.cheonil.orderRsv.entity.OrderRsv;

public interface OrderRsvRepo
    extends JpaRepository<OrderRsv, Long>, JpaSpecificationExecutor<OrderRsv> {

  /** 스케줄러 멱등성 체크 — 같은 (템플릿, 예약시각) 으로 이미 생성됐는지. */
  boolean existsByRsvTmplSeqAndRsvAt(Short rsvTmplSeq, java.time.OffsetDateTime rsvAt);

  /** 단건 조회 — 수동 트리거 후 결과 반환용. unique 제약 (rsv_tmpl_seq, rsv_at) 보장. */
  Optional<OrderRsv> findByRsvTmplSeqAndRsvAt(Short rsvTmplSeq, java.time.OffsetDateTime rsvAt);
}
