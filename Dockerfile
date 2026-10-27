# syntax=docker/dockerfile:1
#
# Образ «Доски объявлений»: Spring Boot + собранный фронтенд React Admin.
# Многоступенчатая сборка: frontend (Node) -> backend (JDK + Gradle) -> runtime (только JRE).
# Отдельная цель `test` прогоняет тесты приложения: docker build --target test .
#
# BASE_REGISTRY — откуда брать базовые образы. mirror.gcr.io — публичное зеркало
# Docker Hub без лимитов на анонимные скачивания; для Docker Hub напрямую:
#   --build-arg BASE_REGISTRY=docker.io/library
ARG BASE_REGISTRY=mirror.gcr.io/library

# --- 1. Фронтенд ---------------------------------------------------------------
FROM ${BASE_REGISTRY}/node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# --- 2. Бэкенд: зависимости ----------------------------------------------------
FROM ${BASE_REGISTRY}/eclipse-temurin:27-jdk AS deps
WORKDIR /app
# Сначала только обёртка Gradle и описание сборки: слой с зависимостями
# пересобирается, лишь когда меняются они, а не код
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --quiet dependencies > /dev/null
COPY src ./src

# --- 2a. Тесты (docker build --target test) ------------------------------------
FROM deps AS test
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon test

# --- 2b. Сборка jar ------------------------------------------------------------
FROM deps AS backend
# Собранный фронтенд отдаётся самим Spring Boot как статика (а наружу его кеширует nginx)
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar /app/app.jar

# --- 3. Рантайм ----------------------------------------------------------------
FROM ${BASE_REGISTRY}/eclipse-temurin:27-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app -u 10001 app \
    && mkdir -p /app/logs && chown app:app /app/logs
COPY --from=backend /app/app.jar /app/app.jar
USER 10001

ENV SPRING_PROFILES_ACTIVE=prod \
    MANAGEMENT_SERVER_PORT=9090 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# 8080 — трафик приложения, 9090 — actuator (health-пробы и метрики)
EXPOSE 8080 9090

HEALTHCHECK --interval=10s --timeout=3s --start-period=90s --retries=3 \
    CMD wget -qO- http://127.0.0.1:9090/actuator/health/readiness > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "app"]
