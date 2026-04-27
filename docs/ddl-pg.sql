create table public.m_setting
(
    seq    smallserial
        primary key,
    config jsonb not null
);

comment on table public.m_setting is '시스템 전역 설정';

comment on column public.m_setting.seq is 'PK';

comment on column public.m_setting.config is '설정 값 (JSON)';

alter table public.m_setting
    owner to root;

create table public.m_store_category
(
    seq     smallserial
        primary key,
    nm      varchar(45) not null
        unique,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on table public.m_store_category is '가게 카테고리';

comment on column public.m_store_category.seq is 'PK';

comment on column public.m_store_category.nm is '카테고리명';

comment on column public.m_store_category.options is '부가 옵션 (JSON)';

comment on column public.m_store_category.reg_at is '등록 시각';

comment on column public.m_store_category.mod_at is '수정 시각';

alter table public.m_store_category
    owner to root;

create table public.m_store
(
    seq       smallserial
        primary key,
    ctg_seq   smallint    not null,
    nm        varchar(45) not null
        unique,
    addr      varchar(200),
    cmt       varchar(1000),
    latitude  double precision,
    longitude double precision,
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now()
);

comment on table public.m_store is '가게 (지점)';

comment on column public.m_store.seq is 'PK';

comment on column public.m_store.ctg_seq is '가게 카테고리 FK (m_store_category.seq)';

comment on column public.m_store.nm is '가게명';

comment on column public.m_store.addr is '주소';

comment on column public.m_store.cmt is '비고';

comment on column public.m_store.latitude is '위도';

comment on column public.m_store.longitude is '경도';

comment on column public.m_store.options is '부가 옵션 (JSON)';

comment on column public.m_store.reg_at is '등록 시각';

comment on column public.m_store.mod_at is '수정 시각';

alter table public.m_store
    owner to root;

create index idx_store_ctg
    on public.m_store (ctg_seq);

create table public.m_menu_category
(
    seq     smallserial
        primary key,
    nm      varchar(20) not null
        unique,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on table public.m_menu_category is '메뉴 카테고리';

comment on column public.m_menu_category.seq is 'PK';

comment on column public.m_menu_category.nm is '카테고리명';

comment on column public.m_menu_category.options is '부가 옵션 (JSON)';

comment on column public.m_menu_category.reg_at is '등록 시각';

comment on column public.m_menu_category.mod_at is '수정 시각';

alter table public.m_menu_category
    owner to root;

create table public.m_menu
(
    seq     smallserial
        primary key,
    ctg_seq smallint    not null,
    nm      varchar(45) not null
        unique,
    nm_s    varchar(10),
    price   integer     not null,
    cmt     varchar(1000),
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on table public.m_menu is '메뉴';

comment on column public.m_menu.seq is 'PK';

comment on column public.m_menu.ctg_seq is '메뉴 카테고리 FK (m_menu_category.seq)';

comment on column public.m_menu.nm is '메뉴명';

comment on column public.m_menu.nm_s is '짧은 이름 / 약칭';

comment on column public.m_menu.price is '가격 (원)';

comment on column public.m_menu.cmt is '비고';

comment on column public.m_menu.options is '부가 옵션 (JSON)';

comment on column public.m_menu.reg_at is '등록 시각';

comment on column public.m_menu.mod_at is '수정 시각';

alter table public.m_menu
    owner to root;

create index idx_menu_ctg
    on public.m_menu (ctg_seq);

create table public.m_expense_category
(
    seq     serial
        primary key,
    path    ltree       not null
        unique,
    nm      varchar(50) not null,
    options jsonb
);

comment on table public.m_expense_category is '지출 카테고리 (ltree 계층 구조)';

comment on column public.m_expense_category.seq is 'PK';

comment on column public.m_expense_category.path is '계층 경로 (예: food.meat.beef)';

comment on column public.m_expense_category.nm is '카테고리명';

comment on column public.m_expense_category.options is '부가 옵션 (JSON)';

alter table public.m_expense_category
    owner to root;

create index idx_exps_ctg_path
    on public.m_expense_category using gist (path);

create table public.m_unit
(
    seq         smallserial
        primary key,
    nm          varchar(40)           not null,
    is_unit_cnt boolean default false not null
);

comment on table public.m_unit is '단위 (kg, 박스, 개 등)';

comment on column public.m_unit.seq is 'PK';

comment on column public.m_unit.nm is '단위명';

comment on column public.m_unit.is_unit_cnt is '단위 수량 사용 여부';

alter table public.m_unit
    owner to root;

create table public.m_product
(
    seq          serial
        primary key,
    prd_info_seq smallint not null,
    unit_seq     smallint not null,
    cmt varchar(1000),
    unit_cnts    numeric(6, 2)[]
);

comment on table public.m_product is '제품 (제품 정보 + 단위 조합)';

comment on column public.m_product.seq is 'PK';

comment on column public.m_product.prd_info_seq is '제품 정보 FK (m_product_info.seq)';

comment on column public.m_product.unit_seq is '단위 FK (m_unit.seq)';

comment on column public.m_product.unit_cnts is '단위별 수량 옵션 배열 (예: {1, 5, 10})';

alter table public.m_product
    owner to root;

create index idx_product_prd_info
    on public.m_product (prd_info_seq);

create index idx_product_unit
    on public.m_product (unit_seq);

create table public.m_order_rsv_tmpl
(
    seq       smallserial
        primary key,
    store_seq smallint                                       not null,
    nm        varchar(40)                                    not null,
    amount    integer                                        not null,
    rsv_time  time                                           not null,
    day_types day_type[]                                     not null,
    cmt       varchar(1000),
    active    boolean                  default true,
    start_dt  date                     default (now())::date not null,
    end_dt    date,
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now()
);

comment on table public.m_order_rsv_tmpl is '예약 주문 템플릿 (단골 반복 예약)';

comment on column public.m_order_rsv_tmpl.seq is 'PK';

comment on column public.m_order_rsv_tmpl.store_seq is '가게 FK (m_store.seq)';

comment on column public.m_order_rsv_tmpl.nm is '템플릿명 (단골 식별용)';

comment on column public.m_order_rsv_tmpl.amount is '예약 금액';

comment on column public.m_order_rsv_tmpl.rsv_time is '예약 시각 (HH:MM)';

comment on column public.m_order_rsv_tmpl.day_types is '반복 요일 배열 (예: {MON, WED, FRI})';

comment on column public.m_order_rsv_tmpl.cmt is '비고';

comment on column public.m_order_rsv_tmpl.active is '활성 여부 (false면 반복 중단)';

comment on column public.m_order_rsv_tmpl.start_dt is '패턴 시작일';

comment on column public.m_order_rsv_tmpl.end_dt is '패턴 종료일 (무기한이면 NULL)';

comment on column public.m_order_rsv_tmpl.options is '부가 옵션 (JSON)';

comment on column public.m_order_rsv_tmpl.reg_at is '등록 시각';

comment on column public.m_order_rsv_tmpl.mod_at is '수정 시각';

alter table public.m_order_rsv_tmpl
    owner to root;

create index idx_order_rsv_tmpl_store
    on public.m_order_rsv_tmpl (store_seq);

create table public.m_order_rsv_menu
(
    menu_seq     smallint not null,
    rsv_tmpl_seq smallint not null,
    price        integer  not null,
    cnt          smallint not null,
    primary key (menu_seq, rsv_tmpl_seq)
);

comment on table public.m_order_rsv_menu is '예약 템플릿 메뉴 (단골이 고정적으로 주문하는 메뉴)';

comment on column public.m_order_rsv_menu.menu_seq is '메뉴 FK (m_menu.seq)';

comment on column public.m_order_rsv_menu.rsv_tmpl_seq is '예약 템플릿 FK (m_order_rsv_tmpl.seq)';

comment on column public.m_order_rsv_menu.price is '메뉴 가격 (당시 시점)';

comment on column public.m_order_rsv_menu.cnt is '수량';

alter table public.m_order_rsv_menu
    owner to root;

create index idx_order_rsv_menu_tmpl
    on public.m_order_rsv_menu (rsv_tmpl_seq);

create table public.t_order
(
    seq       bigserial
        primary key,
    store_seq smallint                                               not null,
    rsv_seq   bigint,
    amount    integer                                                not null,
    status    order_status             default 'READY'::order_status not null,
    order_at  timestamp with time zone default now()                 not null,
    cooked_at timestamp with time zone,
    cmt       varchar(1000),
    mod_at    timestamp with time zone default now()                 not null
);

comment on table public.t_order is '주문';

comment on column public.t_order.seq is 'PK';

comment on column public.t_order.store_seq is '가게 FK (m_store.seq)';

comment on column public.t_order.rsv_seq is '예약 FK (t_order_rsv.seq) - 예약 주문일 때만 값 존재';

comment on column public.t_order.amount is '주문 금액';

comment on column public.t_order.status is '주문 상태 (READY/COOKED/PAID)';

comment on column public.t_order.order_at is '주문 시각';

comment on column public.t_order.cooked_at is '조리 완료 시각';

comment on column public.t_order.cmt is '비고';

comment on column public.t_order.mod_at is '수정 시각';

alter table public.t_order
    owner to root;

create index idx_order_store
    on public.t_order (store_seq);

create index idx_order_rsv
    on public.t_order (rsv_seq);

create table public.t_order_menu
(
    menu_seq  smallint not null,
    order_seq bigint   not null,
    price     integer  not null,
    cnt       smallint not null,
    primary key (menu_seq, order_seq)
);

comment on table public.t_order_menu is '주문 메뉴';

comment on column public.t_order_menu.menu_seq is '메뉴 FK (m_menu.seq)';

comment on column public.t_order_menu.order_seq is '주문 FK (t_order.seq)';

comment on column public.t_order_menu.price is '메뉴 가격 (주문 시점)';

comment on column public.t_order_menu.cnt is '수량';

alter table public.t_order_menu
    owner to root;

create index idx_order_menu_order
    on public.t_order_menu (order_seq);
    
create table public.t_order_rsv
(
    seq          bigserial
        primary key,
    store_seq    smallint                                                not null,
    rsv_tmpl_seq smallint,
    amount       integer                                                 not null,
    rsv_at       timestamp with time zone                                not null,
    status       rsv_status               default 'RESERVED'::rsv_status not null,
    cmt          varchar(1000),
    reg_at       timestamp with time zone default now()                  not null,
    mod_at       timestamp with time zone default now()                  not null
);

comment on table public.t_order_rsv is '예약 주문 인스턴스 (일회성 또는 템플릿 기반 반복)';

comment on column public.t_order_rsv.seq is 'PK';

comment on column public.t_order_rsv.store_seq is '가게 FK (m_store.seq)';

comment on column public.t_order_rsv.rsv_tmpl_seq is '예약 템플릿 FK (m_order_rsv_tmpl.seq) - NULL이면 일회성 예약';

comment on column public.t_order_rsv.amount is '예약 금액';

comment on column public.t_order_rsv.rsv_at is '예약 일시';

comment on column public.t_order_rsv.status is '예약 상태 (RESERVED/COMPLETED/CANCELED)';

comment on column public.t_order_rsv.reg_at is '등록 시각';

comment on column public.t_order_rsv.mod_at is '수정 시각';

alter table public.t_order_rsv
    owner to root;

create index idx_order_rsv_store
    on public.t_order_rsv (store_seq);

create index idx_order_rsv_tmpl
    on public.t_order_rsv (rsv_tmpl_seq);

create table public.t_order_rsv_menu
(
    menu_seq smallint not null,
    rsv_seq  bigint   not null,
    price    integer  not null,
    cnt      smallint not null,
    primary key (menu_seq, rsv_seq)
);

comment on table public.t_order_rsv_menu is '예약 주문 메뉴';

comment on column public.t_order_rsv_menu.menu_seq is '메뉴 FK (m_menu.seq)';

comment on column public.t_order_rsv_menu.rsv_seq is '예약 FK (t_order_rsv.seq)';

comment on column public.t_order_rsv_menu.price is '메뉴 가격 (예약 시점)';

comment on column public.t_order_rsv_menu.cnt is '수량';

alter table public.t_order_rsv_menu
    owner to root;

create index idx_order_rsv_menu_rsv
    on public.t_order_rsv_menu (rsv_seq);

create table public.t_payment
(
    seq       bigserial
        primary key,
    order_seq bigint                            not null,
    amount    integer                           not null,
    pay_type  pay_type default 'CASH'::pay_type not null,
    pay_at    timestamp with time zone          not null
);

comment on table public.t_payment is '결제';

comment on column public.t_payment.seq is 'PK';

comment on column public.t_payment.order_seq is '주문 FK (t_order.seq)';

comment on column public.t_payment.amount is '결제 금액';

comment on column public.t_payment.pay_type is '결제 수단 (CASH/CARD)';

comment on column public.t_payment.pay_at is '결제 시각';

alter table public.t_payment
    owner to root;

create index idx_payment_order
    on public.t_payment (order_seq);

create table public.t_expense
(
    seq        bigserial
        primary key,
    ctg_seq    integer                  not null,
    store_seq  smallint,
    nm         varchar(50)              not null,
    amount     integer                  not null,
    expense_at timestamp with time zone not null,
    cmt        varchar(400),
    options    jsonb,
    mod_at     timestamp with time zone default now()
);

comment on table public.t_expense is '지출';

comment on column public.t_expense.seq is 'PK';

comment on column public.t_expense.ctg_seq is '지출 카테고리 FK (m_expense_category.seq)';

comment on column public.t_expense.store_seq is '가게 FK (m_store.seq)';

comment on column public.t_expense.nm is '지출 항목명';

comment on column public.t_expense.amount is '지출 금액';

comment on column public.t_expense.expense_at is '지출 시각';

comment on column public.t_expense.cmt is '비고';

comment on column public.t_expense.options is '부가 옵션 (JSON)';

comment on column public.t_expense.mod_at is '수정 시각';

alter table public.t_expense
    owner to root;

create index idx_expense_ctg
    on public.t_expense (ctg_seq);

create index idx_expense_store
    on public.t_expense (store_seq);

create table public.t_expense_product
(
    exps_seq bigint   not null,
    prd_seq  integer  not null,
    cnt      smallint not null,
    price    bigint   not null,
    unit_cnt smallint,
    cmt      varchar(400),
    primary key (exps_seq, prd_seq)
);

comment on table public.t_expense_product is '지출 제품 상세';

comment on column public.t_expense_product.exps_seq is '지출 FK (t_expense.seq)';

comment on column public.t_expense_product.prd_seq is '제품 FK (m_product.seq)';

comment on column public.t_expense_product.cnt is '수량';

comment on column public.t_expense_product.price is '가격';

comment on column public.t_expense_product.unit_cnt is '단위 수량';

comment on column public.t_expense_product.cmt is '비고';

alter table public.t_expense_product
    owner to root;

create index idx_expense_product_prd
    on public.t_expense_product (prd_seq);

create table public.m_brand
(
    seq     smallserial
        primary key,
    path    ltree       not null
        unique,
    nm      varchar(45) not null,
    nm_s    varchar(20),
    cmt     varchar(500),
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on table public.m_brand is '브랜드 마스터 (ltree 계층 구조)';

comment on column public.m_brand.seq is 'PK';

comment on column public.m_brand.path is '계층 경로 (예: domestic.cj.haechandle)';

comment on column public.m_brand.nm is '브랜드명';

comment on column public.m_brand.nm_s is '약칭';

comment on column public.m_brand.cmt is '비고';

comment on column public.m_brand.options is '부가 옵션 (JSON)';

comment on column public.m_brand.reg_at is '등록 시각';

comment on column public.m_brand.mod_at is '수정 시각';

alter table public.m_brand
    owner to root;

create index idx_brand_path
    on public.m_brand using gist (path);

create table public.m_ingredient_category
(
    seq     smallserial
        primary key,
    path    ltree       not null
        unique,
    nm      varchar(30) not null,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on table public.m_ingredient_category is '식자재 카테고리 (ltree 계층 구조)';

comment on column public.m_ingredient_category.seq is 'PK';

comment on column public.m_ingredient_category.path is '계층 경로 (예: meat.beef)';

comment on column public.m_ingredient_category.nm is '카테고리명';

comment on column public.m_ingredient_category.options is '부가 옵션 (JSON)';

comment on column public.m_ingredient_category.reg_at is '등록 시각';

comment on column public.m_ingredient_category.mod_at is '수정 시각';

alter table public.m_ingredient_category
    owner to root;

create index idx_ingd_ctg_path
    on public.m_ingredient_category using gist (path);

create table public.m_ingredient
(
    seq     smallserial
        constraint m_ingredient2_pkey
            primary key,
    ctg_seq smallint,
    nm      varchar(100) not null,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on column public.m_ingredient.seq is 'PK';

comment on column public.m_ingredient.nm is '식자재명';

comment on column public.m_ingredient.options is '부가 옵션 (JSON)';

comment on column public.m_ingredient.reg_at is '등록 시각';

comment on column public.m_ingredient.mod_at is '수정 시각';

alter table public.m_ingredient
    owner to root;

create index idx_ingredient_ctg
    on public.m_ingredient (ctg_seq);

create table public.m_product_info
(
    seq       smallserial
        constraint m_product_info2_pkey
            primary key,
    ingd_seq  smallint     not null,
    brand_seq smallint,
    nm        varchar(100) not null,
    cmt       varchar(200),
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now()
);

comment on table public.m_product_info is '제품 정보 (식자재의 상품 정보)';

comment on column public.m_product_info.seq is 'PK';

comment on column public.m_product_info.ingd_seq is '식자재 FK (m_ingredient.seq)';

comment on column public.m_product_info.nm is '제품명';

comment on column public.m_product_info.cmt is '비고';

comment on column public.m_product_info.options is '부가 옵션 (JSON)';

comment on column public.m_product_info.reg_at is '등록 시각';

comment on column public.m_product_info.mod_at is '수정 시각';

alter table public.m_product_info
    owner to root;

create index idx_product_info_ingd
    on public.m_product_info (ingd_seq);

create index idx_product_info_brand
    on public.m_product_info (brand_seq);

