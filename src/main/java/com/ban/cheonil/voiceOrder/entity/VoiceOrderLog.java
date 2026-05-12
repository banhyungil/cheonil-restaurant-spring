package com.ban.cheonil.voiceOrder.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * 음성 주문 감사/디버깅 로그 (Level 1).
 *
 * <p>매 음성 주문 시도마다 1 row. 오디오 자체는 디스크 볼륨에 저장하고 {@link #audioPath} 로 참조. retention 정책에 따라 daily
 * cleanup 으로 파일과 row 모두 삭제.
 */
@Getter
@Setter
@Entity
@Table(name = "t_voice_order_log")
public class VoiceOrderLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long seq;

  /** 주문 생성 성공 시 t_order.seq. 실패면 null. order 삭제 시 FK SET NULL. */
  @Column(name = "order_seq")
  private Long orderSeq;

  /** 오디오 파일 호스트 상대경로 (volume 마운트 기준). 예: 2026/05/12/uuid.webm */
  @Column(name = "audio_path", nullable = false, length = 500)
  private String audioPath;

  @Column(name = "audio_mime", nullable = false, length = 50)
  private String audioMime;

  @Column(name = "audio_size_bytes", nullable = false)
  private Integer audioSizeBytes;

  /** 최종 성공한 STT 엔진명 — "WHISPER" / "GOOGLE". 둘 다 실패 시 null. */
  @Column(name = "engine_used", length = 20)
  private String engineUsed;

  @Column(name = "whisper_text", columnDefinition = "text")
  private String whisperText;

  @Column(name = "google_text", columnDefinition = "text")
  private String googleText;

  /** parse 에 실제 쓴 텍스트. */
  @Column(name = "final_text", columnDefinition = "text")
  private String finalText;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  /**
   * 로그 생성 시각. 한 번 set 후 변경 불가 (감사 특성).
   *
   * <p>{@code @CreationTimestamp} — Hibernate 가 persist 시점에 자동 주입. {@code @ColumnDefault} 만으로는
   * INSERT 시 NULL 명시되어 NOT NULL 위반 발생 (DB DEFAULT 는 컬럼 누락 시에만 동작). 둘 다 두는 이유:
   *
   * <ul>
   *   <li>{@code @CreationTimestamp}: 런타임 자동 주입
   *   <li>{@code @ColumnDefault}: DDL 생성 시 DEFAULT 절 포함 (수동 SQL INSERT 도 안전)
   * </ul>
   */
  @CreationTimestamp
  @ColumnDefault("now()")
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
