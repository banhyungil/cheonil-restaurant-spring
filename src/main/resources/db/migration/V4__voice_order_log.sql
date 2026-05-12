-- ---------------------------------------------------------------------
-- t_voice_order_log — 음성 주문 감사/디버깅 로그
--
-- 매 음성 주문 시도마다 1 row.
--   - 성공: order_seq 가 created order 의 seq
--   - 실패: order_seq null, error_message 에 사유
-- 오디오 파일 자체는 디스크/볼륨에 저장 (audio_path 참조).
-- retention: 정책 (기본 90일) 에 따라 daily cleanup 으로 파일+row 삭제.
-- ---------------------------------------------------------------------
create table t_voice_order_log
(
    seq              bigserial primary key,
    -- 주문 생성 성공 시 t_order.seq. 실패면 null. order 삭제 시 FK SET NULL 로 로그는 유지.
    order_seq        bigint,
    -- 오디오 파일 호스트 상대경로 (volume 마운트 기준). 예: 2026/05/12/abc-uuid.webm
    audio_path       varchar(500)                            not null,
    audio_mime       varchar(50)                             not null,
    audio_size_bytes integer                                 not null,
    -- 최종 성공한 STT 엔진. 둘 다 실패 시 null.
    engine_used      varchar(20),
    -- 단계별 STT 결과 — fine-tuning 용 데이터 수집 + 비교 분석.
    whisper_text     text,
    google_text      text,
    -- parse 에 실제 쓴 텍스트 (engine_used 와 일치).
    final_text       text,
    -- 양쪽 다 실패한 경우의 에러 메시지.
    error_message    text,
    created_at       timestamp with time zone default now() not null,
    foreign key (order_seq) references t_order (seq) on delete set null
);
comment on table t_voice_order_log is '음성 주문 로그 — 감사/디버깅/추후 fine-tuning 데이터';
create index idx_voice_order_log_created_at on t_voice_order_log (created_at);
create index idx_voice_order_log_order_seq on t_voice_order_log (order_seq);
