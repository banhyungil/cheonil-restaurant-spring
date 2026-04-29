# Node → Spring 서버 마이그레이션 방안

기준 소스
- Node 프로젝트: `../Cheonil-Restaurant-Node`
- 매핑 단일 소스: `../Cheonil-Restaurant-Node/scripts/migrate-mariadb-to-pg.ts`
- DDL: `../Cheonil-Restaurant-Node/src/resources/db/ddl-pg.sql`, `ddl-pg-comments.sql`
- 현재 Spring 베이스: Spring Boot 4.0.5 / JDK 25 / JPA / WebMVC / WebSocket / springdoc / PostgreSQL

## 1. 큰 그림

`migrate-mariadb-to-pg.ts`는 단순 데이터 이관 스크립트가 아니라 **Node ↔ PG 스키마의 정식 매핑표**다. JPA 엔티티 / DTO / 리포지토리는 모두 이 스크립트의 `mappings[]` 정의를 기준으로 작성한다.

| 영역 | Node (Express + Sequelize, MariaDB) | Spring (Boot 4 + JPA, PostgreSQL) |
|---|---|---|
| 진입점 | `src/index.ts` + `server.ts` | `CheonilRestaurantSpringApplication` |
| 라우팅 | `src/routes/*.ts` (`Router()`) | `@RestController` (controller 패키지) |
| 비즈니스 | `src/services/*.ts` | `@Service` (service 패키지) |
| 데이터 | Sequelize 모델 + `model.findAll/include` | `@Entity` + Spring Data JPA Repository, 동적 쿼리는 Querydsl, PG 전용 기능은 네이티브 쿼리 |
| 트랜잭션 | `sequelize.transaction(...)` + cls-hooked | `@Transactional` |
| WebSocket | `ws` (`ws-server.ts` broadcast) | `spring-boot-starter-websocket` (`WebSocketHandler` + `TextMessage` broadcast) |
| OpenAPI | `swagger-ui-express` + 정적 yaml | `springdoc-openapi-starter-webmvc-ui` (어노테이션 기반 자동 생성) |
| DB 마이그레이션 | `DbService.init()` + `dbChanges.ts` 자체 구현 | Flyway 또는 Liquibase 도입 권장 (DDL은 이미 `ddl-pg.sql` 존재) |
| 환경 분리 | `src/config/{dev,prod}.ts` + `.env` | `application-{profile}.yaml` |

## 2. 패키지 구조 (목표)

```
ban.gil.cheonil
├─ CheonilRestaurantSpringApplication
├─ config/        # WebSocketConfig, JacksonConfig(JSONB), OpenApiConfig
├─ controller/    # *Controller — Express routes 1:1 대응
├─ service/       # *Service — Express services + 라우트 내부 로직 흡수
├─ repo/          # *Repository (JPA) + DTO/Projection
├─ model/         # @Entity (PG 테이블 1:1)
├─ dto/           # 요청/응답 바디 (record)
├─ enums/         # OrderStatus, PayType, DayType, RsvStatus
└─ common/        # 예외, 에러코드, 공통 응답
```

> 현재 `model/`, `repo/`, `dto/`가 비어있는데 명명 충돌을 정리한다: **JPA 엔티티는 `model/`**, **JPA Repository는 `repo/`**, **요청/응답 DTO는 `dto/`** 로 통일 (현재 `repo/ExpenseDto.java`는 위치만 잘못됨 → `dto/`로 이동).

## 3. 엔티티 매핑 — `migrate-mariadb-to-pg.ts` 기준

스크립트 `mappings[]`가 정의한 PG 테이블/컬럼을 그대로 `@Entity`로 옮긴다.

### 3.1 마스터 (m_)

| Node 모델 | PG 테이블 | Spring `@Entity` (제안) | 비고 |
|---|---|---|---|
| Setting | m_setting | Setting | `config jsonb` → `JsonNode` (Hypersistence Utils 또는 자체 Converter) |
| StoreCategory | m_store_category | StoreCategory | `nm` ← `name` |
| Store | m_store | Store | `addr/cmt/latitude/longitude` |
| MenuCategory | m_menu_category | MenuCategory | |
| Menu | m_menu | Menu | `nm_s` ← Node `abv` |
| ExpenseCategory | m_expense_category | ExpenseCategory | **`path` 컬럼은 PG `ltree`** — 별도 처리 필요 (3.4 참고) |
| Supply | m_ingredient | Ingredient | **모델명 변경** (Supply → Ingredient) |
| ProductInfo | m_product_info | ProductInfo | `ingd_seq` ← `suplSeq` |
| Unit | m_unit | Unit | `is_unit_cnt boolean` |
| Product | m_product | Product | `unit_cnts NUMERIC(6,2)[]` → `BigDecimal[]` (배열 매핑) |
| OrderRsv | m_order_rsv_tmpl | OrderRsvTmpl | **테이블명 변경**, `day_types day_type[]` 배열 |
| OrderMenuRsv | m_order_rsv_menu | OrderRsvMenu | 복합키 (menu_seq, rsv_tmpl_seq) |

### 3.2 트랜잭션 (t_)

| Node 모델 | PG 테이블 | Spring `@Entity` | 비고 |
|---|---|---|---|
| MyOrder | t_order | Order | 클래스명 충돌 주의 → `OrderEntity` 권장. 신규 컬럼 `rsv_seq` (예약 연결, NULL 허용) |
| OrderMenu | t_order_menu | OrderMenu | 복합키 (menu_seq, order_seq) |
| Payment | t_payment | Payment | `pay_type` enum |
| Expense | t_expense | Expense | |
| ExpenseProduct | t_expense_product | ExpenseProduct | 복합키 (exps_seq, prd_seq) |
| (신규) | t_order_rsv | OrderRsv | Node에는 없는 신규 테이블 — 마이그레이션 대상 아님 |
| (신규) | t_order_rsv_menu | OrderRsvMenu | 신규 |

### 3.3 ENUM

PG 측 `CREATE TYPE`으로 정의된 enum을 Java enum + `@JdbcType(PostgreSQLEnumJdbcType.class)` 또는 `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` 으로 매핑.

- `order_status` (READY/COOKED/PAID)
- `pay_type` (CASH/CARD)
- `day_type` (MON~SUN) — 배열로 사용됨
- `rsv_status` (RESERVED/COMPLETED/CANCELED)

### 3.4 PG 전용 타입 처리

| PG 타입 | Java 매핑 전략 |
|---|---|
| `jsonb` | Hibernate 6 기본 `@JdbcTypeCode(SqlTypes.JSON)` + `Map<String,Object>` 또는 도메인 record |
| `text[]` / `numeric[]` / `day_type[]` | `@JdbcTypeCode(SqlTypes.ARRAY)` + 배열 필드, 또는 `List<>` + Converter |
| `ltree` (m_expense_category.path) | Hibernate 기본 매핑 없음 → `@Type` 커스텀 또는 `String` + `@ColumnTransformer(read="path::text", write="?::ltree")` |
| `timestamptz` | `OffsetDateTime` 또는 `Instant` |
| `time` (m_order_rsv_tmpl.rsv_time) | `LocalTime` |

### 3.5 컬럼 명명 규칙

- DB는 snake_case (`reg_at`, `mod_at`, `ctg_seq`, ...)
- Java 필드는 camelCase (`regAt`, `modAt`, `ctgSeq`)
- 자동 변환: Spring Boot 기본 `SpringPhysicalNamingStrategy`로 충분 (별도 어노테이션 불필요한 케이스 다수)

## 4. API 엔드포인트 매핑

`src/common/Paths.ts` + `src/routes/*.ts` 기준으로 `/api` prefix는 Spring `server.servlet.context-path: /api` 또는 `@RequestMapping("/api")` 로 통일.

| Path | Node Router | Spring Controller |
|---|---|---|
| /menu | menu.ts | MenuController |
| /menuCategory | menuCategory.ts | MenuCategoryController |
| /store | store.ts | StoreController |
| /storeCategory | storeCategory.ts | StoreCategoryController |
| /order | order.ts | OrderController |
| /payment | payment.ts | PaymentController |
| /placeCategory | placeCategory.ts | PlaceCategoryController |
| /supply | supply.ts | IngredientController (의미 변경 반영) |
| /productInfos | productInfo.ts | ProductInfoController |
| /unit | unit.ts | UnitController |
| /products | product.ts | ProductController |
| /expenses | expense.ts | ExpenseController (현재 스켈레톤만 존재) |
| /expenseCategories | expenseCategory.ts | ExpenseCategoryController |
| /setting | setting.ts | SettingController |

각 컨트롤러는 Node 라우터의 `GET / GET /:seq / POST / PATCH /:seq / DELETE /:seq` + 도메인 특수 엔드포인트를 1:1 이식.

### 4.1 주의가 필요한 라우트

- **`POST /order/account`**: raw SQL UNION 쿼리. JPQL/Querydsl로 표현 어려움 → **`@Query(nativeQuery=true)` + DTO projection** 으로 그대로 이식. (날짜 파라미터 바인딩 필수, 현재 스크립트의 문자열 연결은 **SQL Injection 취약** — Spring 이식 시 `:fromDt`/`:toDt` 바인딩으로 교정.)
- **`POST /order/collect/:seq`, `/cancelCollect/:seq`**: 다중 INSERT/UPDATE → `@Transactional` 서비스 메서드.
- **`POST /order/collect`, `/cancelCollect` (배열)**: Node는 `Promise.all`. JPA에서는 단일 트랜잭션 내 순차 처리 + 부분 실패 정책 결정 필요.
- **`GET /expenses?expand=category,store,expsPrds`**: Sequelize의 `include` 동적 분기. JPA에서는 **Querydsl** + `leftJoin(...).fetchJoin()` 분기 (또는 `EntityGraph` 별도 메서드 분리).
- **쿼리스트링 `whereOptions[col][op]=val` (qs 파싱)**: Sequelize `Op` 직접 노출 — **이식하지 않고 단순 평면 파라미터로 변경**.
  - 변경 전: `?whereOptions[status][in]=COOKED,PAID&whereOptions[orderAt][between]=...`
  - 변경 후: `?status=COOKED,PAID&fromOrderAt=2026-04-01&toOrderAt=2026-04-30&storeName=...&page=0&size=20&sort=orderAt,asc`
  - 컨트롤러는 `@RequestParam` 으로 직접 수신 (`Optional<>` 또는 nullable record), 정렬/페이징은 Spring `Pageable` 로 표준화.
  - 처리: 파라미터 1~2개면 Spring Data 메서드 (`findByStatusAndOrderAtBetween`), 옵셔널 조합이 늘면 Querydsl `BooleanBuilder`.
  - 필터 조합이 단순해지므로 **연산자(eq/in/between/like)는 컬럼별로 고정 결정** — 클라이언트가 동적으로 op를 고르지 않음.
  - 클라이언트 호환성 필요 시 한 번만 어댑터 레이어에서 변환.

### 4.2 쿼리 작성 전략 — Querydsl + Raw SQL 혼용

| 케이스 | 도구 | 본 프로젝트 적용 예 |
|---|---|---|
| 단순 CRUD, 1~2개 평면 파라미터 | Spring Data JPA 메서드 (`findByStatusAndOrderAtBetween`) + `Pageable` | `/menu`, `/store`, `/order` 기본 검색 |
| 옵셔널 파라미터 3개 이상 조합 | **Querydsl** + `BooleanBuilder` (각 파라미터 null 체크) | `/order` 다조건, `/expenses` 검색 |
| 동적 fetch join | **Querydsl** | `/expenses?expand=...` |
| PG 전용 연산 (`ltree @>`, `jsonb ->`, 윈도우) | **`@Query(nativeQuery=true)`** | `m_expense_category` 트리 조회 |
| UNION / 복잡 집계 | **네이티브 쿼리 + record projection** | `/order/account` |
| 단순 JOIN + 정적 절 | JPQL `@Query` | 보조 |

> 원칙: **문자열 SQL 조립은 금지**. 분기 있는 쿼리는 Querydsl, PG 고유 기능은 네이티브 + 바인딩 파라미터.

## 5. WebSocket

Node `ws-server.ts`는 단순 broadcast. Spring 등가:

```java
@Configuration
@EnableWebSocket
class WebSocketConfig implements WebSocketConfigurer {
    public void registerWebSocketHandlers(WebSocketHandlerRegistry r) {
        r.addHandler(new BroadcastHandler(), "/ws").setAllowedOrigins("*");
    }
}
```

`BroadcastHandler extends TextWebSocketHandler` 에서 세션 목록 유지 + `handleTextMessage`에서 전 세션 broadcast.

## 6. DB 마이그레이션 전략

Node의 `DbService` 자체 버저닝 (Setting.config.dbVersion + `changes/*.sql`) 은 Spring으로 이식하지 말고 **Flyway 도입**:

1. `src/main/resources/db/migration/V1__init.sql` ← `ddl-pg.sql` + `ddl-pg-comments.sql` + ENUM 생성
2. `V2__fk_indexes.sql` ← `ddl-pg-fk.sql` (있으면)
3. 이후 변경은 `V3__xxx.sql` 추가
4. 의존성: `org.flywaydb:flyway-core` + `flyway-database-postgresql`

데이터 이관은 1회성: 기존 `migrate-mariadb-to-pg.ts` 스크립트를 그대로 사용 (Spring 운영 시작 전).

## 7. 설정 (application.yaml)

```yaml
spring:
  application:
    name: cheonil-restaurant-spring
  datasource:
    url: jdbc:postgresql://localhost:5432/cheonil
    username: root
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate     # Flyway가 스키마 관리 → JPA는 검증만
    properties:
      hibernate.jdbc.time_zone: Asia/Seoul
      hibernate.format_sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  servlet:
    context-path: /api

springdoc:
  swagger-ui:
    path: /api-docs
```

프로파일은 `application-dev.yaml`, `application-prod.yaml`로 분리 (Node `config/development.ts` 대응).

## 8. 의존성 추가 (build.gradle.kts)

```kotlin
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.x")  // jsonb/array/ltree 매핑 지원
implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")                // 동적 쿼리 (raw SQL과 병행)
annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
annotationProcessor("jakarta.persistence:jakarta.persistence-api")
annotationProcessor("jakarta.annotation:jakarta.annotation-api")
```

## 9. 작업 순서 (Phase)

### Phase 0 — 인프라 (0.5일)
- [ ] application.yaml 프로파일 구성 + datasource
- [ ] Flyway 도입, `V1__init.sql`로 `ddl-pg.sql` 흡수
- [ ] `model/`, `repo/`, `dto/` 패키지 정리 (현 `repo/ExpenseDto.java` → `dto/`)
- [ ] JSONB / Array / Enum / ltree 컨버터 공통 설정
- [ ] Querydsl 의존성 + APT 설정, `build/generated` 소스 디렉터리 등록

### Phase 1 — 마스터 엔티티 (1~2일)
- [ ] Setting, StoreCategory, Store, MenuCategory, Menu
- [ ] ExpenseCategory (ltree), Ingredient, ProductInfo, Unit, Product
- [ ] OrderRsvTmpl, OrderRsvMenu
- [ ] 각 `JpaRepository` + 기본 CRUD 컨트롤러

### Phase 2 — 트랜잭션 엔티티 (1~2일)
- [ ] OrderEntity, OrderMenu, Payment
- [ ] Expense, ExpenseProduct
- [ ] OrderRsv, OrderRsvMenu (신규)

### Phase 3 — 도메인 서비스 (2~3일)
- [ ] OrderService.collect / cancelCollect / getOrder
- [ ] `/order` 검색 — 평면 `@RequestParam` (status/from/to/storeName) + `Pageable`, 옵셔널 다조건이면 Querydsl `BooleanBuilder`
- [ ] `/order/account` 네이티브 쿼리 (바인딩 교정, SQL Injection 제거)
- [ ] `/expenses?expand=...` Querydsl 동적 fetch join

### Phase 4 — 부속 (1일)
- [ ] WebSocket broadcast
- [ ] springdoc 어노테이션 정리, `/api-docs` 노출
- [ ] 전역 예외 핸들러 (`@RestControllerAdvice`) — Node `RouteError`/`ResponseError` 대응

### Phase 5 — 검증 (1~2일)
- [ ] 데이터 이관 (`migrate-mariadb-to-pg.ts --truncate` 1회 실행)
- [ ] Spring Boot 기동 → Flyway validate 통과 확인
- [ ] 주요 엔드포인트 API 테스트 (Postman/REST Docs)
- [ ] Node 서버 응답과 비교 (회귀 테스트)

## 10. 위험 요소 / 결정 필요

1. **`MyOrder` 클래스명**: Java `Order` 키워드 아님이지만 `org.springframework.data.domain.Order` 와 import 충돌 위험. → `OrderEntity` 권장.
2. **ltree 매핑**: Hibernate 기본 미지원. Hypersistence Utils의 `LTreeType` 또는 `@ColumnTransformer` 커스텀 결정 필요.
3. **`whereOptions` 동적 쿼리**: Sequelize `Op`/`qs` 중첩 파싱은 **이식하지 않음**. 평면 `@RequestParam` + `Pageable` 로 단순화, 클라이언트 호환성 필요 시 한 번만 어댑터에서 변환. 컬럼별 연산자(eq/in/between)는 서버에서 고정.
4. **부분 실패 정책**: Node `/order/collect` 배열 처리는 일부 실패해도 다른 항목 진행. Spring에서는 트랜잭션 경계와 함께 정책 명시 필요.
5. **타임존**: 기존 `timestamp without time zone` 혼재 가능성 → DDL 확인 후 `OffsetDateTime` 통일.
6. **인증/인가**: 현 Node 코드에 인증 미발견. 이번 마이그레이션 범위에 포함할지 결정 필요.

## 11. 참고 — 매핑 대조표 한 줄 요약

`migrate-mariadb-to-pg.ts:84~265` 의 `mappings[]` 배열이 본 작업의 정본. 엔티티 작성 시 항상 이 배열의 `to / columns / transform`을 확인하여 컬럼 순서·이름·타입 변환 규칙을 일치시킬 것.