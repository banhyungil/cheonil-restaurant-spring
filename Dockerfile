# syntax=docker/dockerfile:1.6
# =====================================================================
# Multi-stage build — Java 25 + Gradle bootJar.
# =====================================================================

# ─── Stage 1: Build ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Wrapper + 빌드 스크립트 먼저 복사 — 소스 변경 시 dependency 캐시 재사용
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew \
 && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 + bootJar 빌드 (테스트는 CI 책임이라 컨테이너 빌드에선 skip)
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
 && cp build/libs/*.jar /app/app.jar

# ─── Stage 2: Runtime ───────────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# Spring Boot 기본 포트 — application.yaml 의 server.port 와 일치
EXPOSE 8080

# 컨테이너에서 stop 시 SIGTERM 으로 graceful shutdown
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
