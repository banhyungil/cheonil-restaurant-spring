-- ---------------------------------------------------------------------
-- m_store — 판매처 / 구매처 구분
--   is_sale     : 판매처 — 주문 / 예약 화면의 매장 선택 대상
--   is_purchase : 구매처 — 지출 등록의 구입처 선택 대상
--   둘 다 true 가능 (판매처이면서 구입처). 둘 다 false 는 의미 없으므로 금지.
--   기존 매장은 모두 주문 이력이 있는 판매처 → default 로 is_sale = true, is_purchase = false.
-- ---------------------------------------------------------------------
alter table m_store
    add column is_sale     boolean default true  not null,
    add column is_purchase boolean default false not null,
    add constraint ck_store_sale_or_purchase check (is_sale or is_purchase);

comment on column m_store.is_sale is '판매처 여부 (주문 대상)';
comment on column m_store.is_purchase is '구매처 여부 (지출 구입처)';
