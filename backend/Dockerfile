# ── Build stage ──────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 의존성 캐시 최적화: 빌드 스크립트 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 후 실행 가능한 boot jar 생성 (테스트는 DB 필요 → 생략)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ── Runtime stage ────────────────────────────────────────────
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
