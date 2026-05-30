-- ---------------------------------------------------------------------
-- t_payment.vat — 부가세 (공급가 amount 와 분리 보관)
--   amount = 공급가(주문금액 기준), vat = 부가세, 실수령액 = amount + vat
--   CARD 결제만 vat = round(amount * 0.1), CASH 는 0
--   기존 행은 부가세 개념 도입 전이므로 0 으로 백필 (소급 적용 안 함)
-- ---------------------------------------------------------------------
alter table t_payment
    add column vat integer default 0 not null;

comment on column t_payment.vat is '부가세 (공급가 amount 와 분리, 실수령액 = amount + vat)';
