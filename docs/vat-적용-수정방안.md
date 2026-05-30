# 카드 결제 부가세(VAT) 적용 — 수정 방안

> 작성일: 2026-05-30
> 목적: 카드 결제 시 공급가의 10% 부가세를 부과하고, 화면에는 부가세를 **별도 표기하지 않고 금액에 합산(실수령액)** 하여 표시한다.

---

## 1. 핵심 설계 결정

### 1-1. 데이터 모델 — `vat` 컬럼 분리 (확정)

`t_payment.amount` 에 부가세를 섞지 않고 **`vat` 컬럼을 분리**한다.

| 필드     | 의미                                            |
| -------- | ----------------------------------------------- |
| `amount` | 공급가 (주문금액 기준)                          |
| `vat`    | 부가세 (CARD만 `round(amount * 0.1)`, CASH는 0) |
| 실수령액 | `amount + vat`                                  |

**분리하는 이유**

- 분할결제 합계검증(`sum(amount) == order.amount`)을 그대로 유지 가능
- 매출 집계 기준(공급가/실수령액)을 나중에 선택적으로 조정 가능
- 회계상 공급가/부가세 추적 가능

### 1-2. 화면 표기 — 합산 표시 (확정)

화면에서는 부가세를 따로 보여주지 않고 `amount + vat` 한 값으로 표기한다.
단, **사용자 입력(분할결제 다이얼로그)과 주문금액(orderAmount) 기준 합계는 공급가 그대로 유지**한다.

### 1-3. ⚠️ 미결정 — KPI/통계 집계 기준 (검토 필요)

거래내역의 "결제금액" 컬럼은 gross(`amount+vat`)로 표시하기로 했으나,
**KPI 카드(현금/카드/총매출)와 통계 차트**를 gross로 할지 net(공급가)로 둘지 결정 필요.

| 안           | 내용                                          | 영향                                                                                         |
| ------------ | --------------------------------------------- | -------------------------------------------------------------------------------------------- |
| **(A)**      | 거래내역 컬럼만 gross, KPI/매출은 공급가 유지 | 백엔드 집계 변경 없음. 단 "결제금액 컬럼 합 ≠ 카드 KPI" 불일치 발생 (카드분만 컬럼이 10% 큼) |
| **(B) 추천** | KPI·총매출·통계까지 gross로 통일              | 백엔드 `SalesService` 집계를 `amount+vat` 기준으로 수정. 화면 전체가 실수령액으로 일관       |

> ✅ **결정 결과 기재란:** ********\_\_\_\_********

---

## 2. 백엔드 변경 (✅ 구현 완료 — 모델/저장 부분)

> `BUILD SUCCESSFUL` 컴파일 검증 완료. 단 §1-3 (B)안 선택 시 집계 부분 추가 작업 필요.

| #   | 파일                                                          | 변경 내용                                                                                                                                 | 상태 |
| --- | ------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ---- |
| B1  | `db/migration/V5__payment_vat.sql`                            | `t_payment` 에 `vat integer not null default 0` 컬럼 추가 + **기존 카드 결제 백필**: 과거 `amount`(VAT 포함) → 공급가/부가세 분리 (`vat=round(amount/11)`, `amount-=vat`, 실수령 불변). 현금은 vat 0 유지 | ✅   |
| B2  | `payment/entity/Payment.java`                                 | `vat` 필드 추가 (`@NotNull`, default 0)                                                                                                   | ✅   |
| B3  | `payment/PaymentService.java`                                 | `saveOnePayment` 에서 `calcVat()` 호출 — 단건/일괄/분할 모두 단일 지점 적용. `VAT_RATE = 0.1f`, CARD만 `Math.round(amount*0.1)`, CASH는 0 | ✅   |
| B4  | `payment/dto/PaymentRes.java`                                 | 응답에 `vat` 노출                                                                                                                         | ✅   |
| B5  | `sales/dto/PaymentRes.java` + `SalesService.toPaymentResList` | 거래내역 결제 entry에 `vat` 노출                                                                                                          | ✅   |

### 동작 결과

| payType | amount(공급가) | vat   | 실수령액 |
| ------- | -------------- | ----- | -------- |
| CASH    | 10,000         | 0     | 10,000   |
| CARD    | 10,000         | 1,000 | 11,000   |

- 분할결제 합계검증 `sum(amount) == order.amount` → **손대지 않음, 그대로 유효**
- 매출 KPI 집계 `sum(amount)` → 현재는 **공급가 기준 유지** (§1-3 (B) 선택 시 변경)

### (B)안 선택 시 추가 백엔드 작업

| 파일                                                | 변경 내용                                                              |
| --------------------------------------------------- | ---------------------------------------------------------------------- |
| `sales/SalesService.java` `aggregateBy` (L253 부근) | `Payment::getAmount` → `amount + vat` 합산으로 변경 (PayMethodSummary) |
| `sales/SalesService.java` `totalSales` 등 매출 합계 | gross 기준 반영 여부 확인                                              |
| `sales/stats/SalesStatsService.java`                | 결제유형/매출 통계 집계도 gross 반영 여부 확인                         |

---

## 3. 프론트 변경 (⏳ 예정)

### 그룹 0 — 타입 보강 (선행 필수)

| 파일                                | 변경                                                      |
| ----------------------------------- | --------------------------------------------------------- |
| `src/types/sales.ts` `PaymentEntry` | `vat: number` 필드 추가 (현재 누락. 백엔드는 이미 내려줌) |
| `src/types/payment.ts` `Payment`    | ✅ 이미 `vat` 추가됨                                      |

### 그룹 1 — 결제별 금액 합산 표시 (프론트만으로 처리)

| #   | 화면                             | 파일:라인                                                  | 변경                            |
| --- | -------------------------------- | ---------------------------------------------------------- | ------------------------------- |
| F1  | 정산 탭 · 거래내역 "결제금액"    | `settlement/TransactionTable.vue:154` (`payAmountSum`)     | `p.amount` → `p.amount + p.vat` |
| F2  | 주문내역관리 그리드 "결제금액"   | `sales/SalesGridTable.vue:132` (`payAmountSum`, 중복 함수) | `p.amount` → `p.amount + p.vat` |
| F3  | 결제수단 chip · 분할결제 tooltip | `settlement/PayTypeChip.vue:53`                            | `p.amount` → `p.amount + p.vat` |

> 💡 `payAmountSum` 이 F1/F2 두 곳에 복붙돼 있음 → 공용 유틸 `grossAmount(p) = p.amount + p.vat` 로 추출 후 재사용 권장.

### 그룹 2 — KPI/통계 (§1-3 (B)안 선택 시에만, 백엔드 집계 수정 후 자동 반영)

| #   | 화면                         | 파일:라인                                   |
| --- | ---------------------------- | ------------------------------------------- |
| F4  | 정산 탭 KPI (현금/카드/미수) | `settlement/SalesSummaryCards.vue:33,45,57` |
| F5  | 주문내역관리 KPI (현금/카드) | `sales/SalesGridSummaryCards.vue:27,39`     |
| F6  | 통계 · 결제유형 도넛 등      | `sales/StatsBasicView.vue` 및 차트류        |

> F4~F6은 백엔드가 gross로 내려주면 **프론트 코드 변경 없이** 자동 반영됨 (필드 의미만 바뀜).

---

## 4. 변경하지 않는 곳 (의도적 제외)

| 화면                     | 파일:라인                                  | 이유                                                                |
| ------------------------ | ------------------------------------------ | ------------------------------------------------------------------- |
| 분할결제 입력 다이얼로그 | `settlement/SplitPaymentDialog.vue:35,130` | 사용자 입력은 공급가, `합계 == orderAmount` 검증도 공급가 기준 유지 |
| 거래내역 footer 합계     | `settlement/TransactionTable.vue:83`       | `orderAmount` 기준 (payment 아님)                                   |
| 수금 탭 헤더/선택 합계   | `settlement/CollectionTable.vue:179,190`   | `orderAmount` 기준 (payment 아님)                                   |
| 그리드 선택 합계         | `sales/SalesGridTable.vue:77`              | `orderAmount` 기준 (payment 아님)                                   |

---

## 5. 작업 순서 (제안)

1. **§1-3 KPI 기준 결정** (A or B) ← 선행
2. 백엔드: (B 선택 시) `SalesService`/`SalesStatsService` 집계 gross 수정
3. 프론트 그룹 0: `PaymentEntry` 타입에 `vat` 추가
4. 프론트 그룹 1: `grossAmount` 유틸 추출 → F1/F2/F3 적용
5. (B 선택 시) 그룹 2 화면 확인 (대부분 자동 반영)
6. 검증: 단건/일괄/분할 결제 → 거래내역·KPI 금액 정합성 확인

---

## 6. 체크리스트

- [x] §1-3 KPI 집계 기준 결정 (A/B) → **B로**
- [x] (B) 백엔드 집계 수정
  - `PayMethodSummary` → `{amount(공급가/net), vat, count}` — **vat 별도 반환**, 표시 합산은 프론트
  - `SalesService.aggregateBy` → amount=Σnet, vat=Σvat (현금 vat=0)
  - `SalesService.summary` / `ordersSummary` totalSales → `Σ공급가 + Σvat` (gross). 매출=현금+카드+미수 정합 유지
  - `SalesStatsService`: 결제유형 도넛(payParts), 매출/점포/시간대/추이 → 실수령액(`grossOf`/`vatByOrder`)
  - 메뉴/카테고리 통계는 **net 유지** (부가세는 결제수단 부가금 → 메뉴 귀속 불가). 전일대비(prevSales)도 공급가 기준 유지
  - ✅ `BUILD SUCCESSFUL`
- [x] 프론트 `PaymentEntry.vat` 타입 추가 (사용자 완료)
- [~] `grossAmount` 공용 유틸 추출 → **인라인(`p.amount + p.vat`)으로 처리** (추후 유틸 추출 가능)
- [x] F1 TransactionTable 결제금액
- [x] F2 SalesGridTable 결제금액
- [x] F3 PayTypeChip 분할 tooltip
- [x] (B) F4/F5 KPI 카드(현금/카드) → `amount + vat` 표시로 수정 (SalesSummaryCards / SalesGridSummaryCards). 매출/순매출은 totalSales(gross) 그대로
- [x] (B) F6 통계 도넛 → 백엔드 payParts gross 반영으로 자동 적용 (프론트 변경 불필요), `type-check` 통과
- [ ] 결제 흐름별 금액 정합성 수동 검증 (앱 실행)
