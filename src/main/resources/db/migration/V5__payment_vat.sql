-- ---------------------------------------------------------------------
-- t_payment.vat — 부가세 (공급가 amount 와 분리 보관)
--   amount = 공급가(주문금액 기준), vat = 부가세, 실수령액 = amount + vat
--   CARD 결제만 vat = round(amount * 0.1), CASH 는 0
--
--   [기존 데이터 백필] — 카드 결제, 2026-05-15 기준 분기
--   · 5/01 이전: amount 부가세 포함 → 공급가/부가세 분리 (vat=round(amount/11), amount-=vat, 실수령 불변)
--   · 5/01 이후: amount 공급가(부가세 미반영) → 부가세 가산 (vat=round(amount*0.1), amount 유지)
--   현금은 부가세 없음 → vat 0 유지.
-- ---------------------------------------------------------------------
alter table t_payment
    add column vat integer default 0 not null;

comment on column t_payment.vat is '부가세 (공급가 amount 와 분리, 실수령액 = amount + vat)';

-- ① 5/01 이전 — amount(부가세 포함) → 공급가 + 부가세 분리.
--    SET 우변은 모두 갱신 전 amount 기준으로 평가되므로 한 문장으로 안전.
update t_payment
set vat    = round(amount / 11.0)::int,
    amount = amount - round(amount / 11.0)::int
where pay_type = 'CARD'
and pay_at <  '2026-05-15 00:00:00+09'
and vat = 0;

-- ② 5/01 이후 — amount(공급가, 부가세 미반영)에 부가세만 가산. amount 는 유지.
update t_payment
set vat = round(amount * 0.1)::int
where pay_type = 'CARD'
and pay_at >= '2026-05-01 00:00:00+09'
and vat = 0;
