create table public.m_setting
(
    code           varchar(40) not null
        primary key,
    default_config jsonb       not null,
    user_config    jsonb,
    mod_at         timestamp with time zone default now()
);

comment on table public.m_setting is '시스템 전역 설정 (code 기반 lookup, default/user override 분리)';

comment on column public.m_setting.code is 'PK / 설정 코드 (SettingCode enum 과 동기화)';

comment on column public.m_setting.default_config is '시스템 기본값 (seed). 운영자도 변경 가능';

comment on column public.m_setting.user_config is '사용자 override 값. null = 기본값 사용. restore = NULL 로 set';

comment on column public.m_setting.mod_at is '마지막 수정 시각';

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

alter table public.m_store_category
    owner to root;

create table public.m_store
(
    seq       smallserial
        primary key,
    ctg_seq   smallint                              not null,
    nm        varchar(45)                           not null
        unique,
    addr      varchar(200),
    cmt       varchar(1000),
    latitude  double precision,
    longitude double precision,
    active    boolean                  default true not null,
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now()
);

comment on table public.m_store is '가게 (지점)';

comment on column public.m_store.active is '활성화 여부 — false 면 영업/주문 페이지에서 제외 (관리자 페이지엔 노출)';

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

alter table public.m_menu_category
    owner to root;

create table public.m_menu
(
    seq     smallserial
        primary key,
    ctg_seq smallint                              not null,
    nm      varchar(45)                           not null
        unique,
    nm_s    varchar(10),
    price   integer                               not null,
    cmt     varchar(1000),
    active  boolean                  default true not null,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

comment on table public.m_menu is '메뉴';

comment on column public.m_menu.active is '활성화 여부 — false 면 영업/주문 페이지에서 제외 (관리자 페이지엔 노출)';

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

alter table public.m_expense_category
    owner to root;

create index idx_exps_ctg_path
    on public.m_expense_category using gist (path);

create table public.m_unit
(
    seq         smallserial
        primary key,
    nm            varchar(40)           not null,
    is_unit_cnt   boolean default false not null,
    base_unit_seq smallint,
    base_factor   numeric(10, 4)
);

comment on table public.m_unit is '단위 (kg, 박스, 개 등)';

comment on column public.m_unit.base_unit_seq is '기준 단위 (g → kg). NULL 이면 환산 불가';

comment on column public.m_unit.base_factor is '기준 단위 환산계수 (g → 0.001)';

alter table public.m_unit
    owner to root;

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

alter table public.m_ingredient_category
    owner to root;

create index idx_ingd_ctg_path
    on public.m_ingredient_category using gist (path);

create table public.m_ingredient
(
    seq     smallserial
        primary key,
    ctg_seq smallint,
    nm      varchar(100) not null,
    options jsonb,
    reg_at  timestamp with time zone default now(),
    mod_at  timestamp with time zone default now()
);

alter table public.m_ingredient
    owner to root;

create index idx_ingredient_ctg
    on public.m_ingredient (ctg_seq);

create table public.m_product_info
(
    seq       smallserial
        primary key,
    ingd_seq  smallint     not null,
    brand_seq smallint,
    nm        varchar(100) not null,
    cmt       varchar(200),
    options   jsonb,
    reg_at    timestamp with time zone default now(),
    mod_at    timestamp with time zone default now(),
    constraint uq_product_info
        unique (ingd_seq, nm)
);

comment on table public.m_product_info is '제품 정보 (식자재의 상품 정보)';

alter table public.m_product_info
    owner to root;

create index idx_product_info_ingd
    on public.m_product_info (ingd_seq);

create index idx_product_info_brand
    on public.m_product_info (brand_seq);

create table public.m_product
(
    seq          serial
        primary key,
    prd_info_seq smallint not null,
    unit_seq     smallint not null,
    cmt          varchar(1000),
    unit_cnts    numeric(6, 2)[],
    constraint uq_product
        unique (prd_info_seq, unit_seq)
);

comment on table public.m_product is '제품 (제품 정보 + 단위 조합)';

alter table public.m_product
    owner to root;

create index idx_product_prd_info
    on public.m_product (prd_info_seq);

create index idx_product_unit
    on public.m_product (unit_seq);

create table public.m_order_rsv_tmpl
(
    seq             smallserial
        primary key,
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

comment on table public.m_order_rsv_tmpl is '예약 주문 템플릿 (단골 반복 예약)';

comment on column public.m_order_rsv_tmpl.auto_order is '예약 자동 생성 시 주문(t_order)도 즉시 생성 여부 — 신뢰 단골에 한해 활성화';

comment on column public.m_order_rsv_tmpl.last_rsv_gen_at is '스케줄러가 마지막으로 이 템플릿에서 예약(t_order_rsv) 을 생성한 시각 — 목록 조회 시 오늘 예약 생성 여부 파악용';

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

comment on table public.m_order_rsv_menu is '예약 템플릿 메뉴';

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
    order_seq    bigint,
    amount       integer                                                 not null,
    rsv_at       timestamp with time zone                                not null,
    status       rsv_status               default 'RESERVED'::rsv_status not null,
    cmt          varchar(1000),
    reg_at       timestamp with time zone default now()                  not null,
    mod_at       timestamp with time zone default now()                  not null,
    constraint uk_order_rsv_tmpl_at
        unique (rsv_tmpl_seq, rsv_at)
);

comment on table public.t_order_rsv is '예약 주문 인스턴스 (일회성 또는 템플릿 기반 반복)';

alter table public.t_order_rsv
    owner to root;

create index idx_order_rsv_store
    on public.t_order_rsv (store_seq);

create index idx_order_rsv_tmpl
    on public.t_order_rsv (rsv_tmpl_seq);

create index idx_order_rsv_order
    on public.t_order_rsv (order_seq);

create table public.t_order_rsv_menu
(
    menu_seq smallint not null,
    rsv_seq  bigint   not null,
    price    integer  not null,
    cnt      smallint not null,
    primary key (menu_seq, rsv_seq)
);

comment on table public.t_order_rsv_menu is '예약 주문 메뉴';

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
    pay_at    timestamp with time zone          not null,
    vat       integer  default 0                not null
);

comment on table public.t_payment is '결제';

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
    mod_at     timestamp with time zone default now(),
    reg_at     timestamp with time zone default now()
);

comment on table public.t_expense is '지출';

alter table public.t_expense
    owner to root;

create index idx_expense_ctg
    on public.t_expense (ctg_seq);

create index idx_expense_store
    on public.t_expense (store_seq);

create unique index uq_expense_store_day
    on public.t_expense (store_seq, ((expense_at at time zone 'Asia/Seoul'::text)::date))
    where (store_seq is not null);

create table public.t_expense_product
(
    seq      bigserial
        primary key,
    exps_seq bigint   not null,
    prd_seq  integer  not null,
    cnt      smallint not null,
    price    integer  not null,
    unit_cnt numeric(6, 2),
    cmt      varchar(400),
    constraint uq_expense_product
        unique nulls not distinct (exps_seq, prd_seq, unit_cnt)
);

comment on table public.t_expense_product is '지출 제품 상세';

comment on column public.t_expense_product.price is '단가 (줄 합계 = cnt * price)';

comment on column public.t_expense_product.unit_cnt is '구입 시 선택한 단위수량 (m_product.unit_cnts 중 하나 또는 직접 입력)';

alter table public.t_expense_product
    owner to root;

create index idx_expense_product_prd
    on public.t_expense_product (prd_seq);

create table public.t_voice_order_log
(
    seq              bigserial
        primary key,
    order_seq        bigint
                                                            references public.t_order
                                                                on delete set null,
    audio_path       varchar(500)                           not null,
    audio_mime       varchar(50)                            not null,
    audio_size_bytes integer                                not null,
    engine_used      varchar(20),
    whisper_text     text,
    google_text      text,
    final_text       text,
    error_message    text,
    created_at       timestamp with time zone default now() not null
);

comment on table public.t_voice_order_log is '음성 주문 로그 — 감사/디버깅/추후 fine-tuning 데이터';

alter table public.t_voice_order_log
    owner to root;

create index idx_voice_order_log_created_at
    on public.t_voice_order_log (created_at);

create index idx_voice_order_log_order_seq
    on public.t_voice_order_log (order_seq);

