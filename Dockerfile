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

# claude CLI 설치 — VoiceOrderService 가 subprocess 로 호출.
# 공식 installer 가 호스트 OS/arch 감지해 적절한 바이너리 다운로드.
# auth 는 docker-compose 에서 ~/.claude 를 /root/.claude 로 mount 함 (이미지에 baked X).
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && curl -fsSL https://claude.ai/install.sh | bash \
 && apt-get clean && rm -rf /var/lib/apt/lists/*
ENV PATH="/root/.local/bin:${PATH}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
