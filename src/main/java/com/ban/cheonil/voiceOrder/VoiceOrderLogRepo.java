package com.ban.cheonil.voiceOrder;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ban.cheonil.voiceOrder.entity.VoiceOrderLog;

public interface VoiceOrderLogRepo extends JpaRepository<VoiceOrderLog, Long> {

  /** retention cleanup — created_at 이 cutoff 이전인 row 들. 파일도 함께 삭제 후 row delete. */
  List<VoiceOrderLog> findByCreatedAtBefore(OffsetDateTime cutoff);

  /** 운영자 페이지 — 최근순 페이지네이션. Pageable 의 sort 로 정렬 지정 (기본 created_at DESC). */
  Page<VoiceOrderLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
