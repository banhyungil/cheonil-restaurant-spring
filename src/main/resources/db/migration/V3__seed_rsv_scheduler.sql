-- =====================================================================
-- V3: m_setting RSV_SCHEDULER seed
-- =====================================================================
-- 예약 스케줄러 lead time (분). 60 = 트리거 시점 기준 60분 후 rsv_time 처리.
-- =====================================================================

insert into m_setting (code, default_config) values
    ('RSV_SCHEDULER', '{"leadMinutes": 60}'::jsonb)
on conflict (code) do nothing;
