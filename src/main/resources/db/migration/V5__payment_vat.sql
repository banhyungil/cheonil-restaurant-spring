-- ---------------------------------------------------------------------
-- t_payment.vat — 부가세 (공급가 amount 와 분리 보관)
--   amount = 공급가(주문금액 기준), vat = 부가세, 실수령액 = amount + vat
--   CARD 결제만 vat = round(amount * 0.1), CASH 는 0
--
--   [기존 데이터 백필]
--   과거 카드 결제는 amount 에 부가세가 포함(VAT 포함가)되어 있었음.
--   → 공급가/부가세로 분리: vat = round(amount/11), amount -= vat. 실수령 총액은 불변.
--   현금은 부가세 없음 → vat 0 유지.
-- ---------------------------------------------------------------------
alter table t_payment
    add column vat integer default 0 not null;

comment on column t_payment.vat is '부가세 (공급가 amount 와 분리, 실수령액 = amount + vat)';

-- 기존 카드 결제 분리 — amount(부가세 포함) → 공급가 + 부가세.
-- SET 우변은 모두 갱신 전 amount 기준으로 평가되므로 한 문장으로 안전.
update t_payment
set vat    = round(amount / 11.0)::int,
    amount = amount - round(amount / 11.0)::int
where pay_type = 'CARD';
