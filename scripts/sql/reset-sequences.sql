-- ============================================================
--  public schema 의 모든 serial / identity 시퀀스를
--  해당 컬럼의 현재 MAX 값으로 재정렬.
--
--  사용 시점:
--    - append 복원 후 (ON CONFLICT DO NOTHING 으로 skip 된 행 때문에
--      시퀀스가 어긋날 수 있음)
--    - 수동 INSERT 후 다음 nextval() 충돌 방지
-- ============================================================
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT n.nspname  AS s,
               c.relname  AS t,
               a.attname  AS col,
               pg_get_serial_sequence(
                   quote_ident(n.nspname) || '.' || quote_ident(c.relname),
                   a.attname
               ) AS seq
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_attribute a ON a.attrelid = c.oid
        WHERE c.relkind = 'r'
          AND n.nspname = 'public'
          AND pg_get_serial_sequence(
                  quote_ident(n.nspname) || '.' || quote_ident(c.relname),
                  a.attname
              ) IS NOT NULL
    LOOP
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT MAX(%I) FROM %I.%I), 1), true)',
            r.seq, r.col, r.s, r.t
        );
        RAISE NOTICE 'sequence % -> MAX(%) of %.%', r.seq, r.col, r.s, r.t;
    END LOOP;
END $$;
