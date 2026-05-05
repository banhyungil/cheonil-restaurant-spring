-- =====================================================================
-- V2: m_setting 기본 seed
-- =====================================================================
-- SettingCode enum 과 동기화. 새 코드 추가 시 별도 migration (V3, V4 ...) 으로 진행.
-- ON CONFLICT DO NOTHING 은 운영 baseline 이후에도 idempotent 하게 동작하기 위함.
-- =====================================================================

insert into m_setting (code, default_config) values
    ('STORE_ORDER',          '{"order": []}'::jsonb),
    ('MENU_ORDER',           '{"order": []}'::jsonb),
    ('STORE_CATEGORY_ORDER', '{"order": []}'::jsonb),
    ('MENU_CATEGORY_ORDER',  '{"order": []}'::jsonb),
    ('OPERATING_HOURS',      '{"startHour": 5, "endHour": 16}'::jsonb)
on conflict (code) do nothing;
