# 추가 필요 의존성 목록

`build.gradle.kts` 기준, Node → Spring 마이그레이션을 위해 추가해야 할 의존성과 그 이유.

## 1. 현재 보유 의존성 (변경 불필요)

| 의존성 | 용도 |
|---|---|
| `spring-boot-starter-webmvc` | REST 컨트롤러 (Express Router 대체) |
| `spring-boot-starter-data-jpa` | ORM (Sequelize 대체) |
| `spring-boot-starter-websocket` | WebSocket (`ws` 패키지 대체) |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI/Swagger UI (`swagger-ui-express` 대체) |
| `postgresql` (runtime) | PG JDBC 드라이버 |
| `lombok` | 보일러플레이트 제거 |
| `spring-boot-devtools` | 개발 시 hot reload |

## 2. 추가 필수 의존성

### 2.1 Flyway — DB 마이그레이션 도구

```kotlin
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
```

**왜 필요한가**
- Node `DbService.init()` + `dbChanges.ts` 자체 버저닝 시스템을 대체.
- `ddl-pg.sql` / `ddl-pg-comments.sql` / `ddl-pg-fk.sql` 을 `V1__init.sql` 로 흡수, 이후 변경은 `V2__*.sql` 로 누적.
- `spring.jpa.hibernate.ddl-auto: validate` 와 조합 시, 스키마 변경은 Flyway가 담당하고 JPA는 검증만 수행 → 운영 안정성 확보.
- **`flyway-database-postgresql`**: Flyway 10+ 부터 PG 전용 모듈이 분리되어 별도 추가 필요.

---

### 2.2 Hypersistence Utils — JSONB / Array / ltree 매핑

```kotlin
implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.4")
```

**왜 필요한가**
- 본 스키마는 PG 전용 타입을 적극 사용함:
  - `jsonb` (m_setting.config, m_*.options, t_expense.options 등 다수)
  - `numeric[]`, `day_type[]` (배열 컬럼)
  - `ltree` (m_expense_category.path)
- Hibernate 6 의 `@JdbcTypeCode(SqlTypes.JSON)` 만으로도 jsonb는 일부 처리 가능하나, **`Map<String, Object>` / 도메인 record / List 매핑의 안정성과 편의성**은 Hypersistence Utils가 압도적.
- `LTreeType` 등 PG 전용 타입 핸들러를 즉시 사용 가능.
- 버전: Spring Boot 4.x 는 Hibernate 6.3+ 사용 → `hibernate-63` 아티팩트.

> 대안: 자체 `AttributeConverter` 구현. 단순 케이스만 있으면 가능하나 jsonb + array가 많아 라이브러리 도입이 현실적.

---

### 2.3 Querydsl — 동적 쿼리

```kotlin
implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
annotationProcessor("jakarta.persistence:jakarta.persistence-api")
annotationProcessor("jakarta.annotation:jakarta.annotation-api")
```

**왜 필요한가**
- Node `/order` 라우트의 `whereOptions[col][op]=val` 동적 검색을 타입 안전하게 재작성.
- `/expenses?expand=category,store,expsPrds` 의 동적 fetch join을 `JPAQuery` + `leftJoin(...).fetchJoin()` 으로 처리.
- 컴파일 시 `Q*` 클래스를 자동 생성 → 컬럼/엔티티명 오타가 컴파일 오류로 즉시 드러남. 리팩터링 안전성 확보.
- 대안:
  - **Spring Data Specification** — 추가 의존성 없이 가능, 다만 가독성 떨어짐.
  - **JPQL `@Query`** — 분기 조합 폭발하면 비현실적.
- `jakarta` classifier 필수 (Spring Boot 3+ jakarta.persistence 네임스페이스).

**Querydsl vs Raw SQL — 같이 쓰는 이유**

Querydsl은 raw SQL을 대체하는 게 아니라 **보완**한다. 둘 다 사용한다.

| 상황 | 권장 |
|---|---|
| 일반 CRUD | Spring Data JPA 메서드 |
| 동적 조건 조합 (where 분기) | Querydsl |
| 동적 fetch join | Querydsl |
| PG 전용 기능 (`ltree` `@>`/`<@`, `jsonb` `->`/`@>`, 윈도우 함수, `LATERAL`, `WITH RECURSIVE`) | Raw SQL (`@Query(nativeQuery = true)`) |
| UNION / 복잡 집계 / 리포팅 | Raw SQL + DTO projection |
| 성능 튜닝 (인덱스 힌트, `FOR UPDATE SKIP LOCKED`) | Raw SQL |

본 프로젝트 적용 예:
- `/order` 동적 검색 → **Querydsl**
- `/order/account` UNION 쿼리 → **네이티브 쿼리** (Querydsl로 옮길 이유 없음)
- `m_expense_category` ltree 조회 → **네이티브 쿼리**
- `/menu`, `/store` 등 단순 CRUD → **JPA 메서드**

> 핵심: "raw SQL 대신 Querydsl"이 아니라 **"문자열 조립 대신 Querydsl, 그러나 PG 고유 기능은 raw SQL"**.

**Gradle 설정 보강 — 생성된 Q클래스를 IDE가 인식하게 하기**

```kotlin
// build.gradle.kts
val generated = file("build/generated/sources/annotationProcessor/java/main")

sourceSets {
    main {
        java.srcDirs += generated
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(generated)
}

tasks.named("clean") {
    doLast { generated.deleteRecursively() }
}
```

확인: `./gradlew compileJava` 후 `build/generated/sources/annotationProcessor/java/main/` 하위에 `QExpense.java`, `QOrderEntity.java` 등이 생성되면 정상.

---

### 2.4 Jackson JSR-310 / 시간 모듈 (대부분 자동 포함)

```kotlin
// 보통 자동 포함되지만 명시 시:
implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
```

**왜 필요한가**
- `OffsetDateTime`, `LocalTime`, `LocalDate` 직렬화/역직렬화.
- `spring-boot-starter-webmvc` 가 `jackson-databind` 와 함께 자동 포함 → **명시 불필요**한 것이 일반적이나, 직렬화 이슈 발생 시 점검 포인트.

---

### 2.5 Validation — 요청 DTO 검증

```kotlin
implementation("org.springframework.boot:spring-boot-starter-validation")
```

**왜 필요한가**
- Node `validateQueryParamInfo` / `converNumRes` 같은 수동 파라미터 검증을 `@Valid` + `@NotNull/@Min/@Pattern` 등 표준 어노테이션으로 대체.
- `MethodArgumentNotValidException` 을 `@RestControllerAdvice` 에서 잡아 일관된 에러 응답 변환.

## 3. 추가 선택 의존성

### 3.1 Caffeine — 로컬 캐시

```kotlin
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("com.github.ben-manes.caffeine:caffeine")
```

**왜 필요한가**
- 마스터성 데이터 (Menu, MenuCategory, Store, Unit 등) 가 자주 조회되고 변경 빈도 낮음 → `@Cacheable` 적용 시 응답 속도 개선.
- 도입은 1차 마이그레이션 완료 후 성능 측정 결과에 따라 결정 권장.

---

### 3.2 MapStruct — Entity ↔ DTO 변환

```kotlin
implementation("org.mapstruct:mapstruct:1.6.3")
annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
```

**왜 필요한가**
- Node 라우트는 `model.toJSON()` 으로 그대로 응답. Spring에서는 엔티티를 직접 노출하지 않고 DTO로 변환하는 것이 권장됨.
- 매핑 코드를 컴파일타임에 자동 생성 → 런타임 비용 0, 변경 누락 시 컴파일 오류로 즉시 인지.
- 대안: Java 21+ record + 직접 매핑 메서드 (소규모면 충분).

---

### 3.3 Spring Boot Actuator — 헬스체크 / 모니터링

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

**왜 필요한가**
- `/actuator/health` 로 컨테이너 헬스체크 (Node 프로젝트는 Docker 사용 중 → docker-compose healthcheck 와 연동).
- 운영 환경에서 메트릭 수집 시 필수.

---

### 3.4 TestContainers — 통합 테스트용 PG

```kotlin
testImplementation("org.testcontainers:postgresql:1.20.4")
testImplementation("org.testcontainers:junit-jupiter:1.20.4")
testImplementation("org.springframework.boot:spring-boot-testcontainers")
```

**왜 필요한가**
- jsonb / ltree / array / enum 등 PG 전용 타입 매핑은 H2로 검증 불가. 실제 PG 인스턴스에서 테스트해야 함.
- 회귀 테스트(Node ↔ Spring 응답 비교)를 자동화할 때 필수.

## 4. 정리된 `build.gradle.kts` (제안)

```kotlin
dependencies {
    // 기존
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")     // 추가
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // DB 마이그레이션
    implementation("org.flywaydb:flyway-core")                                    // 추가
    implementation("org.flywaydb:flyway-database-postgresql")                     // 추가

    // PG 전용 타입 매핑
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.4")    // 추가

    // 동적 쿼리 (raw SQL과 병행 사용)
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")                     // 추가
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")                // 추가
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")            // 추가
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")              // 추가

    // 운영
    implementation("org.springframework.boot:spring-boot-starter-actuator")       // 추가 (선택)

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-restdocs")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.testcontainers:postgresql:1.20.4")                    // 추가
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")                 // 추가
    testImplementation("org.springframework.boot:spring-boot-testcontainers")     // 추가
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}
```

## 5. 도입 우선순위

| 순위 | 의존성 | 사유 |
|---|---|---|
| 1 (필수) | Flyway + flyway-database-postgresql | 스키마 일관성, 운영 안전성 |
| 1 (필수) | Hypersistence Utils | jsonb/array/ltree 없으면 다수 엔티티 매핑 불가 |
| 1 (필수) | spring-boot-starter-validation | 입력 검증, 일관된 에러 응답 |
| 1 (필수) | Querydsl | 동적 검색/fetch join 가독성·안전성 (raw SQL과 병행) |
| 2 (강력 권장) | TestContainers | PG 전용 타입의 회귀 보호 |
| 3 (선택) | Actuator | Docker 헬스체크 활용 |
| 3 (선택) | MapStruct | DTO 매핑 규모 확대 시 |
| 3 (선택) | Caffeine 캐시 | 성능 측정 후 도입 |

## 6. 버전 관리 메모

- Spring Boot 4.0.5 사용 → BOM이 다수 의존성 버전 자동 관리. **Flyway, validation 등 starter는 버전 명시 불필요**.
- 서드파티 (Hypersistence, Querydsl, MapStruct, TestContainers) 는 BOM 미관리 → 직접 명시.
- Hypersistence Utils 는 Hibernate 메이저 버전과 1:1 매칭 (`-hibernate-63`) — Spring Boot 업그레이드 시 동시 점검.