/**
 * MariaDB → PostgreSQL 데이터 마이그레이션 스크립트
 *
 * 실행: npx ts-node ./scripts/migrate-mariadb-to-pg.ts
 * 옵션: --dry-run     (PG 쓰기 없이 추출/변환만 검증)
 *       --tables=m_menu,t_order  (특정 테이블만)
 *       --truncate    (PG 대상 테이블 TRUNCATE 후 이관)
 *
 * 사전 요구:
 *   1. npm install pg @types/pg
 *   2. PG DDL 선 적용 (ddl-pg.sql, ddl-pg-comments.sql)
 *   3. MariaDB 원본 접근 가능
 */

// # 1. dry-run으로 변환 검증
// npx ts-node ./scripts/migrate-mariadb-to-pg.ts --dry-run

// # 2. 특정 테이블만 테스트
// npx ts-node ./scripts/migrate-mariadb-to-pg.ts --tables=m_menu --truncate

// # 3. 전체 실행
// npx ts-node ./scripts/migrate-mariadb-to-pg.ts --truncate

import mysql from 'mysql2/promise'
import { Client as PgClient } from 'pg'

// =============================
// 커넥션 설정
// =============================
const MARIA_CONN = {
    host: 'localhost',
    user: 'root',
    port: 3306,
    password: 'nice2122!',
    database: 'cheonil',
}

const PG_CONN = {
    host: 'localhost',
    user: 'root',
    port: 5432,
    password: 'root1!',
    database: 'cheonil',
}

const BATCH_SIZE = 500

// =============================
// CLI 파싱
// =============================
const args = process.argv.slice(2)
const DRY_RUN = args.includes('--dry-run')
const TRUNCATE = args.includes('--truncate')
const TABLE_FILTER = args
    .find((a) => a.startsWith('--tables='))
    ?.slice('--tables='.length)
    .split(',')

// =============================
// 변환 유틸
// =============================
const toBool = (v: unknown): boolean | null => (v === null || v === undefined ? null : !!v)
const toJsonb = (v: unknown): object | null => {
    if (v === null || v === undefined) return null
    if (typeof v === 'object') return v
    try {
        return JSON.parse(String(v))
    } catch {
        return null
    }
}
const nonEmpty = (v: unknown): unknown => (v === '' ? null : v)

// =============================
// 테이블 매핑 (마스터 → 트랜잭션 순서)
// =============================
type Mapping = {
    from: string // MariaDB 테이블명
    to: string // PostgreSQL 테이블명
    columns: string[] // PG 컬럼 순서
    transform: (row: Record<string, unknown>) => unknown[] // row → values 배열 (columns 순서 일치)
}

const mappings: Mapping[] = [
    // m_setting 은 PG DDL 이 자체 seed (code 기반 default/user split) — 마이그레이션 대상 제외.
    // 기존 MariaDB Setting 의 seq/config 구조와 호환 안 됨.
    {
        from: 'StoreCategory',
        to: 'm_store_category',
        columns: ['seq', 'nm', 'options', 'reg_at', 'mod_at'],
        transform: (r) => [r.seq, r.name, toJsonb(r.options), r.createdAt, r.updatedAt],
    },
    {
        from: 'Store',
        to: 'm_store',
        columns: ['seq', 'ctg_seq', 'nm', 'addr', 'cmt', 'latitude', 'longitude', 'active', 'options', 'reg_at', 'mod_at'],
        transform: (r) => [
            r.seq,
            r.ctgSeq,
            r.name,
            r.address,
            nonEmpty(r.cmt),
            r.latitude,
            r.longitude,
            true, // active 신규 컬럼 — 기존 row 는 모두 활성으로
            toJsonb(r.options),
            r.createdAt,
            r.updatedAt,
        ],
    },
    {
        from: 'MenuCategory',
        to: 'm_menu_category',
        columns: ['seq', 'nm', 'options', 'reg_at', 'mod_at'],
        transform: (r) => [r.seq, r.name, toJsonb(r.options), r.createdAt, r.updatedAt],
    },
    {
        from: 'Menu',
        to: 'm_menu',
        columns: ['seq', 'ctg_seq', 'nm', 'nm_s', 'price', 'active', 'cmt', 'options', 'reg_at', 'mod_at'],
        transform: (r) => [
            r.seq,
            r.ctgSeq,
            r.name,
            r.abv, // abv → nm_s
            r.price,
            true, // active 신규 컬럼 — 기존 row 는 모두 활성으로
            nonEmpty(r.cmt),
            toJsonb(r.options),
            r.createdAt,
            r.updatedAt,
        ],
    },
    {
        // 기존 ExpenseCategory는 path VARCHAR였으나 PG는 LTREE
        // path 문자열 형식이 다르면 여기서 '.' 구분자로 변환
        from: 'ExpenseCategory',
        to: 'm_expense_category',
        columns: ['seq', 'path', 'nm', 'options'],
        transform: (r) => [
            r.seq,
            String(r.path ?? '')
                .replace(/\//g, '.')
                .replace(/[^a-zA-Z0-9._]/g, '_'),
            r.name ?? r.nm ?? '(no name)',
            toJsonb(r.options),
        ],
    },
    {
        // Supply → m_ingredient (의미 변경)
        // m_ingredient 의 ctg_seq (NEW, nullable) — MariaDB 원본 없어 NULL.
        from: 'Supply',
        to: 'm_ingredient',
        columns: ['seq', 'ctg_seq', 'nm', 'options', 'reg_at', 'mod_at'],
        transform: (r) => [r.seq, null, r.name, toJsonb(r.options), r.createdAt, r.updatedAt],
    },
    {
        // m_product_info.brand_seq (NEW, nullable) — MariaDB 원본 없어 NULL.
        from: 'ProductInfo',
        to: 'm_product_info',
        columns: ['seq', 'ingd_seq', 'brand_seq', 'nm', 'cmt', 'options', 'reg_at', 'mod_at'],
        transform: (r) => [
            r.seq,
            r.suplSeq, // suplSeq → ingd_seq
            null, // brand_seq 신규 (브랜드 마스터 미운영 시 NULL)
            r.name,
            nonEmpty(r.cmt),
            toJsonb(r.options),
            r.createdAt,
            r.updatedAt,
        ],
    },
    {
        from: 'Unit',
        to: 'm_unit',
        columns: ['seq', 'nm', 'is_unit_cnt'],
        transform: (r) => [r.seq, r.name, toBool(r.isUnitCnt) ?? false],
    },
    {
        // m_product.cmt (NEW, nullable) — MariaDB 원본 없어 NULL.
        from: 'Product',
        to: 'm_product',
        columns: ['seq', 'prd_info_seq', 'unit_seq', 'cmt', 'unit_cnts'],
        transform: (r) => {
            // unitCntList: JSON 문자열 → 정수 배열
            let arr: number[] = []
            if (r.unitCntList) {
                try {
                    const parsed = typeof r.unitCntList === 'string' ? JSON.parse(r.unitCntList) : r.unitCntList
                    if (Array.isArray(parsed)) arr = parsed.map(Number).filter((n) => !Number.isNaN(n))
                } catch {
                    /* ignore */
                }
            }
            return [r.seq, r.prdInfoSeq, r.unitSeq, null, arr]
        },
    },
    {
        // OrderRsv → m_order_rsv_tmpl (단일 dayType → day_types 배열)
        // 신규 필드:
        //   - auto_order (NOT NULL default false) — 기존 row 모두 false 유지
        //   - last_rsv_gen_at (nullable) — 스케줄러 미작동 (NULL)
        //   - start_dt (NOT NULL default now()::date) — null 넘기면 INSERT 실패 → createdAt::date fallback
        from: 'OrderRsv',
        to: 'm_order_rsv_tmpl',
        columns: ['seq', 'store_seq', 'nm', 'amount', 'rsv_time', 'day_types', 'cmt', 'active', 'auto_order', 'start_dt', 'end_dt', 'last_rsv_gen_at', 'options', 'reg_at', 'mod_at'],
        transform: (r) => {
            // start_dt fallback — createdAt 의 date 부분 추출 (없으면 today)
            const startDt =
                r.createdAt instanceof Date
                    ? r.createdAt.toISOString().slice(0, 10)
                    : new Date().toISOString().slice(0, 10)
            return [
                r.seq,
                r.storeSeq,
                `단골-${r.seq}`, // 기존에 nm 없음 → 임시 이름
                r.amount,
                r.rsvTime, // CHAR(5) "18:30" → TIME 자동 캐스팅
                r.dayType ? [r.dayType] : [], // 단일 → 배열
                nonEmpty(r.cmt),
                true, // active
                false, // auto_order — 신규 컬럼, 기존 row 비활성으로 시작
                startDt, // start_dt NOT NULL — fallback
                null, // end_dt (무기한)
                null, // last_rsv_gen_at (스케줄러 미작동)
                toJsonb(r.options),
                r.createdAt,
                r.updatedAt,
            ]
        },
    },
    {
        from: 'OrderMenuRsv',
        to: 'm_order_rsv_menu',
        columns: ['menu_seq', 'rsv_tmpl_seq', 'price', 'cnt'],
        transform: (r) => [r.menuSeq, r.orderRsvSeq, r.price, r.cnt],
    },
    {
        // MyOrder → t_order (rsv_seq은 신규 컬럼 → NULL)
        from: 'MyOrder',
        to: 't_order',
        columns: ['seq', 'store_seq', 'rsv_seq', 'amount', 'status', 'order_at', 'cooked_at', 'cmt', 'mod_at'],
        transform: (r) => [
            r.seq,
            r.storeSeq,
            null, // 신규 컬럼 (예약 주문 연결) → 초기값 NULL
            r.amount,
            r.status ?? 'READY',
            r.orderAt,
            r.cookedAt,
            nonEmpty(r.cmt),
            r.updatedAt,
        ],
    },
    {
        from: 'OrderMenu',
        to: 't_order_menu',
        columns: ['menu_seq', 'order_seq', 'price', 'cnt'],
        transform: (r) => [r.menuSeq, r.orderSeq, r.price, r.cnt],
    },
    // t_order_rsv, t_order_rsv_menu 는 신규 테이블 → 마이그레이션 대상 없음
    {
        from: 'Payment',
        to: 't_payment',
        columns: ['seq', 'order_seq', 'amount', 'pay_type', 'pay_at'],
        transform: (r) => [r.seq, r.orderSeq, r.amount, r.payType ?? 'CASH', r.payAt],
    },
    {
        from: 'Expense',
        to: 't_expense',
        columns: ['seq', 'ctg_seq', 'store_seq', 'nm', 'amount', 'expense_at', 'cmt', 'options', 'mod_at'],
        transform: (r) => [r.seq, r.ctgSeq, r.storeSeq, r.name, r.amount, r.expenseAt, nonEmpty(r.cmt), toJsonb(r.options), r.updatedAt],
    },
    {
        from: 'ExpenseProduct',
        to: 't_expense_product',
        columns: ['exps_seq', 'prd_seq', 'cnt', 'price', 'unit_cnt', 'cmt'],
        transform: (r) => [r.expsSeq, r.prdSeq, r.cnt, r.price, r.unitCnt, nonEmpty(r.cmt)],
    },
]

// =============================
// 실행 로직
// =============================
async function migrate() {
    const maria = await mysql.createConnection(MARIA_CONN)
    const pg = new PgClient(PG_CONN)
    await pg.connect()

    const targets = TABLE_FILTER ? mappings.filter((m) => TABLE_FILTER.includes(m.to) || TABLE_FILTER.includes(m.from)) : mappings

    const summary: Array<{ from: string; to: string; rows: number; status: string }> = []
    const start = Date.now()

    try {
        if (!DRY_RUN) await pg.query('BEGIN')

        for (const m of targets) {
            console.log(`\n▶ ${m.from} → ${m.to}`)

            const [rows] = await maria.query<mysql.RowDataPacket[]>(`SELECT * FROM \`${m.from}\``)
            if (rows.length === 0) {
                console.log('  (empty, skip)')
                summary.push({ from: m.from, to: m.to, rows: 0, status: 'empty' })
                continue
            }

            if (!DRY_RUN && TRUNCATE) {
                await pg.query(`TRUNCATE TABLE ${m.to} CASCADE`)
            }

            const transformed = rows.map((r) => m.transform(r))

            if (DRY_RUN) {
                console.log(`  [dry-run] ${rows.length} rows would be inserted`)
                console.log('  sample:', transformed[0])
                summary.push({ from: m.from, to: m.to, rows: rows.length, status: 'dry-run' })
                continue
            }

            // 배치 INSERT
            for (let i = 0; i < transformed.length; i += BATCH_SIZE) {
                const chunk = transformed.slice(i, i + BATCH_SIZE)
                const placeholders = chunk
                    .map((_, rowIdx) => {
                        const base = rowIdx * m.columns.length
                        return `(${m.columns.map((__, colIdx) => `$${base + colIdx + 1}`).join(', ')})`
                    })
                    .join(', ')
                const flat = chunk.flat()

                // Multi Row Insert
                // placeholder와 row, col 평탄화 배열을 이용해 삽입
                const sql = `INSERT INTO ${m.to} (${m.columns.join(', ')}) VALUES ${placeholders}`
                await pg.query(sql, flat)
            }

            console.log(`  ✓ ${rows.length} rows inserted`)
            summary.push({ from: m.from, to: m.to, rows: rows.length, status: 'ok' })
        }

        // 시퀀스 동기화 (SERIAL PK가 있는 테이블 전체)
        if (!DRY_RUN) {
            console.log('\n▶ 시퀀스 동기화 중...')
            await pg.query(`
                DO $$
                DECLARE r RECORD;
                BEGIN
                    FOR r IN
                        SELECT c.table_name, c.column_name,
                               pg_get_serial_sequence(c.table_name, c.column_name) AS seq_name
                        FROM information_schema.columns c
                        WHERE c.table_schema = 'public'
                          AND pg_get_serial_sequence(c.table_name, c.column_name) IS NOT NULL
                    LOOP
                        EXECUTE format(
                            'SELECT setval(%L, COALESCE((SELECT MAX(%I) FROM %I), 1))',
                            r.seq_name, r.column_name, r.table_name
                        );
                    END LOOP;
                END $$;
            `)
            console.log('  ✓ 완료')

            await pg.query('COMMIT')
        }
    } catch (e) {
        if (!DRY_RUN) await pg.query('ROLLBACK')
        console.error('\n✗ 실패:', e)
        throw e
    } finally {
        await maria.end()
        await pg.end()
    }

    // 요약
    console.log('\n========== 결과 요약 ==========')
    console.table(summary)
    console.log(`총 소요: ${((Date.now() - start) / 1000).toFixed(2)}초`)

    if (DRY_RUN) console.log('\n[dry-run] 실제 쓰기 없음')
}

migrate().catch((e) => {
    console.error(e)
    process.exit(1)
})
