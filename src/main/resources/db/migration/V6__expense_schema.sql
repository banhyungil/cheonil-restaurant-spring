-- ---------------------------------------------------------------------
-- 지출 기능 스키마 정리 (docs/plan/지출 구현.md §2)
--   지출 관련 테이블(t_expense, t_expense_product) 은 데이터 0건 → 재생성/변경 부담 없음.
--   m_product / m_product_info 기존 데이터는 유니크 조합 중복 없음 확인 (2026-09-30 백업).
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- t_expense
--   · reg_at 추가
--   · 같은 일자(KST) + 같은 매장 지출 중복 차단. 매장 없는 지출(공과금 등)은 제외.
--     중복 시 폼에서 기존 지출을 불러와 수정한다 (서버 자동 병합 X).
-- ---------------------------------------------------------------------
alter table t_expense
    add column reg_at timestamp with time zone default now();

create unique index uq_expense_store_day
    on t_expense (store_seq, ((expense_at at time zone 'Asia/Seoul')::date))
    where store_seq is not null;

-- ---------------------------------------------------------------------
-- t_expense_product — 재생성
--   · seq PK — 같은 제품의 다른 규격(600g / 1kg) 을 한 지출에 넣을 수 있도록
--   · 품목 식별 = (지출, 제품, 단위수량). unit_cnt NULL 끼리도 중복으로 본다.
--   · price = 단가 (줄 합계 = cnt * price)
--   · unit_cnt numeric — m_product.unit_cnts 의 소수 규격(1.8 등) 저장
-- ---------------------------------------------------------------------
drop table t_expense_product;

create table t_expense_product
(
    seq      bigserial primary key,
    exps_seq bigint   not null,
    prd_seq  integer  not null,
    cnt      smallint not null,
    price    integer  not null,
    unit_cnt numeric(6, 2),
    cmt      varchar(400),
    constraint uq_expense_product unique nulls not distinct (exps_seq, prd_seq, unit_cnt)
);
comment on table t_expense_product is '지출 제품 상세';
comment on column t_expense_product.price is '단가 (줄 합계 = cnt * price)';
comment on column t_expense_product.unit_cnt is '구입 시 선택한 단위수량 (m_product.unit_cnts 중 하나 또는 직접 입력)';
create index idx_expense_product_prd on t_expense_product (prd_seq);

-- ---------------------------------------------------------------------
-- m_product / m_product_info — 유니크 제약
--   · 제품 키 = (제품정보, 단위). 단위수량(unit_cnts) 은 키가 아닌 규격 목록.
-- ---------------------------------------------------------------------
alter table m_product
    add constraint uq_product unique (prd_info_seq, unit_seq);

alter table m_product_info
    add constraint uq_product_info unique (ingd_seq, nm);

-- 식자재명 유니크 — 제품 등록 시 식자재를 이름으로 찾고 없으면 생성 (기존 데이터 중복 없음 확인)
alter table m_ingredient
    add constraint uq_ingredient unique (nm);

-- ---------------------------------------------------------------------
-- m_unit — 기준 단위 환산 (식자재별 가격 비교용)
--   base_unit_seq NULL = 자기 자신이 기준 단위 (kg, L, 개, 단 ...)
--   비교 그룹 = coalesce(base_unit_seq, seq), 기준단가 = price / (unit_cnt * coalesce(base_factor, 1))
-- ---------------------------------------------------------------------
alter table m_unit
    add column base_unit_seq smallint,
    add column base_factor   numeric(10, 4);

comment on column m_unit.base_unit_seq is '기준 단위 (g → kg). NULL 이면 자기 자신이 기준 단위';
comment on column m_unit.base_factor is '기준 단위 환산계수 (g → 0.001). base_unit_seq 가 있을 때만 값 존재';

update m_unit
set base_unit_seq = (select seq from m_unit where nm = 'kg'),
    base_factor   = 0.001
where nm = 'g';

update m_unit
set base_unit_seq = (select seq from m_unit where nm = 'L'),
    base_factor   = 0.001
where nm = 'ml';
