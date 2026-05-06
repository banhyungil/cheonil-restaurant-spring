-- =====================================================================
-- V1: 베이스라인 스키마
-- =====================================================================
-- 운영 DB (이미 존재하는 DB) 에는 spring.flyway.baseline-on-migrate=true 설정으로
-- 인해 이 V1 은 실행되지 않고, "이미 V1 상태" 로 마킹만 됨. 새로운 환경
-- (로컬 fresh DB / Testcontainers) 에서는 이 스크립트가 그대로 실행됨.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Extension
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS ltree;

-- ---------------------------------------------------------------------
-- PG custom enum types
-- ---------------------------------------------------------------------
CREATE TYPE day_type AS ENUM ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');
CREATE TYPE rsv_status AS ENUM ('RESERVED', 'COMPLETED', 'CANCELED');
CREATE TYPE order_status AS ENUM ('READY', 'COOKED', 'PAID');
CREATE TYPE pay_type AS ENUM ('CASH', 'CARD');

-- ---------------------------------------------------------------------
-- Casts (Hibernate 가 String / String[] 로 보내는 값을 PG enum 으로 자동 변환)
-- ---------------------------------------------------------------------
CREATE CAST (varchar[] AS day_type[]) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS day_type) WITH INOUT AS IMPLICIT;

-- ---------------------------------------------------------------------
-- Function — array_position(day_type[], varchar) overload
-- (PG polymorphic resolver 가 implicit cast 까지 추적 못해 함수 못 찾는 문제 회피)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION array_position(day_type[], varchar)
RETURNS integer AS $$
    SELECT array_position($1, $2::day_type);
$$ LANGUAGE sql IMMUTABLE;

-- ---------------------------------------------------------------------
-- m_setting — 시스템 전역 설정
-- ---------------------------------------------------------------------
create table m_setting
(
    code           varchar(40)              not null primary key,
    default_config jsonb                    not null,
    user_config    jsonb,
    mod_at         timestamp with time zone default now()
);
comment on table m_setting is '시스템 전역 설정 (code 기반 lookup, default/user override 분리)';
comment on column m_setting.code is 'PK / 설정 코드 (SettingCode enum 과 동기화)';
comment on column m_setting.default_config is '시스템 기본값 (seed). 운영자도 변경 가능';
comment on column m_setting.user_config is '사용자 override 값. null = 기본값 사용. restore = NULL 로 set';
comment on column m_setting.mod_at is '마지막 수정 시각';

-- ---------------------------------------------------------------------
-- m_store_category — 가게 카테고리
-- ---------------------------------------------------------------------
create table m_store_category
(
    seq     smallserial primary key,
    nm      varchar(45) not null unique,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);
comment on table m_store_category is '가게 카테고리';

-- ---------------------------------------------------------------------
-- m_store — 가게 (지점)
-- ---------------------------------------------------------------------
create table m_store
(
    seq       smallserial primary key,
    ctg_seq   smallint    not null,
    nm        varchar(45) not null unique,
    addr      varchar(200),
    cmt       varchar(1000),
    latitude  double precision,
    longitude double precision,
    active    boolean                  not null default true,
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now()
);
comment on table m_store is '가게 (지점)';
comment on column m_store.active is '활성화 여부 — false 면 영업/주문 페이지에서 제외 (관리자 페이지엔 노출)';
create index idx_store_ctg on m_store (ctg_seq);

-- ---------------------------------------------------------------------
-- m_menu_category — 메뉴 카테고리
-- ---------------------------------------------------------------------
create table m_menu_category
(
    seq     smallserial primary key,
    nm      varchar(20) not null unique,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);
comment on table m_menu_category is '메뉴 카테고리';

-- ---------------------------------------------------------------------
-- m_menu — 메뉴
-- ---------------------------------------------------------------------
create table m_menu
(
    seq     smallserial primary key,
    ctg_seq smallint    not null,
    nm      varchar(45) not null unique,
    nm_s    varchar(10),
    price   integer     not null,
    cmt     varchar(1000),
    active  boolean                  not null default true,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);
comment on table m_menu is '메뉴';
comment on column m_menu.active is '활성화 여부 — false 면 영업/주문 페이지에서 제외 (관리자 페이지엔 노출)';
create index idx_menu_ctg on m_menu (ctg_seq);

-- ---------------------------------------------------------------------
-- m_expense_category — 지출 카테고리 (ltree)
-- ---------------------------------------------------------------------
create table m_expense_category
(
    seq     serial primary key,
    path    ltree       not null unique,
    nm      varchar(50) not null,
    options jsonb
);
comment on table m_expense_category is '지출 카테고리 (ltree 계층 구조)';
create index idx_exps_ctg_path on m_expense_category using gist (path);

-- ---------------------------------------------------------------------
-- m_unit — 단위
-- ---------------------------------------------------------------------
create table m_unit
(
    seq         smallserial primary key,
    nm          varchar(40)           not null,
    is_unit_cnt boolean default false not null
);
comment on table m_unit is '단위 (kg, 박스, 개 등)';

-- ---------------------------------------------------------------------
-- m_brand — 브랜드 (ltree)
-- ---------------------------------------------------------------------
create table m_brand
(
    seq     smallserial primary key,
    path    ltree       not null unique,
    nm      varchar(45) not null,
    nm_s    varchar(20),
    cmt     varchar(500),
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);
comment on table m_brand is '브랜드 마스터 (ltree 계층 구조)';
create index idx_brand_path on m_brand using gist (path);

-- ---------------------------------------------------------------------
-- m_ingredient_category — 식자재 카테고리 (ltree)
-- ---------------------------------------------------------------------
create table m_ingredient_category
(
    seq     smallserial primary key,
    path    ltree       not null unique,
    nm      varchar(30) not null,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);
comment on table m_ingredient_category is '식자재 카테고리 (ltree 계층 구조)';
create index idx_ingd_ctg_path on m_ingredient_category using gist (path);

-- ---------------------------------------------------------------------
-- m_ingredient — 식자재
-- ---------------------------------------------------------------------
create table m_ingredient
(
    seq     smallserial primary key,
    ctg_seq smallint,
    nm      varchar(100) not null,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);
create index idx_ingredient_ctg on m_ingredient (ctg_seq);

-- ---------------------------------------------------------------------
-- m_product_info — 제품 정보
-- ---------------------------------------------------------------------
create table m_product_info
(
    seq       smallserial primary key,
    ingd_seq  smallint     not null,
    brand_seq smallint,
    nm        varchar(100) not null,
    cmt       varchar(200),
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now()
);
comment on table m_product_info is '제품 정보 (식자재의 상품 정보)';
create index idx_product_info_ingd on m_product_info (ingd_seq);
create index idx_product_info_brand on m_product_info (brand_seq);

-- ---------------------------------------------------------------------
-- m_product — 제품
-- ---------------------------------------------------------------------
create table m_product
(
    seq          serial primary key,
    prd_info_seq smallint not null,
    unit_seq     smallint not null,
    cmt          varchar(1000),
    unit_cnts    numeric(6, 2)[]
);
comment on table m_product is '제품 (제품 정보 + 단위 조합)';
create index idx_product_prd_info on m_product (prd_info_seq);
create index idx_product_unit on m_product (unit_seq);

-- ---------------------------------------------------------------------
-- m_order_rsv_tmpl — 예약 주문 템플릿
-- ---------------------------------------------------------------------
create table m_order_rsv_tmpl
(
    seq             smallserial primary key,
    store_seq       smallint                                       not null,
    nm              varchar(40)                                    not null,
    amount          integer                                        not null,
    rsv_time        time                                           not null,
    day_types       day_type[]                                     not null,
    cmt             varchar(1000),
    active          boolean                  default true,
    auto_order      boolean                  default false         not null,
    start_dt        date                     default (now())::date not null,
    end_dt          date,
    last_rsv_gen_at timestamp with time zone,
    options         jsonb,
    reg_at          timestamp with time zone default now(),
    mod_at          timestamp with time zone default now()
);
comment on table m_order_rsv_tmpl is '예약 주문 템플릿 (단골 반복 예약)';
comment on column m_order_rsv_tmpl.auto_order is '예약 자동 생성 시 주문(t_order)도 즉시 생성 여부 — 신뢰 단골에 한해 활성화';
comment on column m_order_rsv_tmpl.last_rsv_gen_at is '스케줄러가 마지막으로 이 템플릿에서 예약(t_order_rsv) 을 생성한 시각 — 목록 조회 시 오늘 예약 생성 여부 파악용';
create index idx_order_rsv_tmpl_store on m_order_rsv_tmpl (store_seq);

-- ---------------------------------------------------------------------
-- m_order_rsv_menu — 예약 템플릿 메뉴
-- ---------------------------------------------------------------------
create table m_order_rsv_menu
(
    menu_seq     smallint not null,
    rsv_tmpl_seq smallint not null,
    price        integer  not null,
    cnt          smallint not null,
    primary key (menu_seq, rsv_tmpl_seq)
);
comment on table m_order_rsv_menu is '예약 템플릿 메뉴';
create index idx_order_rsv_menu_tmpl on m_order_rsv_menu (rsv_tmpl_seq);

-- ---------------------------------------------------------------------
-- t_order — 주문
-- ---------------------------------------------------------------------
create table t_order
(
    seq       bigserial primary key,
    store_seq smallint                                               not null,
    rsv_seq   bigint,
    amount    integer                                                not null,
    status    order_status             default 'READY'::order_status not null,
    order_at  timestamp with time zone default now()                 not null,
    cooked_at timestamp with time zone,
    cmt       varchar(1000),
    mod_at    timestamp with time zone default now()                 not null
);
comment on table t_order is '주문';
create index idx_order_store on t_order (store_seq);
create index idx_order_rsv on t_order (rsv_seq);

-- ---------------------------------------------------------------------
-- t_order_menu — 주문 메뉴
-- ---------------------------------------------------------------------
create table t_order_menu
(
    menu_seq  smallint not null,
    order_seq bigint   not null,
    price     integer  not null,
    cnt       smallint not null,
    primary key (menu_seq, order_seq)
);
comment on table t_order_menu is '주문 메뉴';
create index idx_order_menu_order on t_order_menu (order_seq);

-- ---------------------------------------------------------------------
-- t_order_rsv — 예약 주문 인스턴스
-- ---------------------------------------------------------------------
create table t_order_rsv
(
    seq          bigserial primary key,
    store_seq    smallint                                                not null,
    rsv_tmpl_seq smallint,
    order_seq    bigint,
    amount       integer                                                 not null,
    rsv_at       timestamp with time zone                                not null,
    status       rsv_status               default 'RESERVED'::rsv_status not null,
    cmt          varchar(1000),
    reg_at       timestamp with time zone default now()                  not null,
    mod_at       timestamp with time zone default now()                  not null,
    constraint uk_order_rsv_tmpl_at unique (rsv_tmpl_seq, rsv_at)
);
comment on table t_order_rsv is '예약 주문 인스턴스 (일회성 또는 템플릿 기반 반복)';
create index idx_order_rsv_store on t_order_rsv (store_seq);
create index idx_order_rsv_tmpl on t_order_rsv (rsv_tmpl_seq);
create index idx_order_rsv_order on t_order_rsv (order_seq);

-- ---------------------------------------------------------------------
-- t_order_rsv_menu — 예약 주문 메뉴
-- ---------------------------------------------------------------------
create table t_order_rsv_menu
(
    menu_seq smallint not null,
    rsv_seq  bigint   not null,
    price    integer  not null,
    cnt      smallint not null,
    primary key (menu_seq, rsv_seq)
);
comment on table t_order_rsv_menu is '예약 주문 메뉴';
create index idx_order_rsv_menu_rsv on t_order_rsv_menu (rsv_seq);

-- ---------------------------------------------------------------------
-- t_payment — 결제
-- ---------------------------------------------------------------------
create table t_payment
(
    seq       bigserial primary key,
    order_seq bigint                            not null,
    amount    integer                           not null,
    pay_type  pay_type default 'CASH'::pay_type not null,
    pay_at    timestamp with time zone          not null
);
comment on table t_payment is '결제';
create index idx_payment_order on t_payment (order_seq);

-- ---------------------------------------------------------------------
-- t_expense — 지출
-- ---------------------------------------------------------------------
create table t_expense
(
    seq        bigserial primary key,
    ctg_seq    integer                  not null,
    store_seq  smallint,
    nm         varchar(50)              not null,
    amount     integer                  not null,
    expense_at timestamp with time zone not null,
    cmt        varchar(400),
    options    jsonb,
    mod_at     timestamp with time zone default now()
);
comment on table t_expense is '지출';
create index idx_expense_ctg on t_expense (ctg_seq);
create index idx_expense_store on t_expense (store_seq);

-- ---------------------------------------------------------------------
-- t_expense_product — 지출 제품 상세
-- ---------------------------------------------------------------------
create table t_expense_product
(
    exps_seq bigint   not null,
    prd_seq  integer  not null,
    cnt      smallint not null,
    price    bigint   not null,
    unit_cnt smallint,
    cmt      varchar(400),
    primary key (exps_seq, prd_seq)
);
comment on table t_expense_product is '지출 제품 상세';
create index idx_expense_product_prd on t_expense_product (prd_seq);
