# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

# Tests run separately with a database; this stage only packages the application.
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x ./gradlew \
    && ./gradlew --no-daemon --console=plain bootJar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build --chown=app:app /workspace/build/libs/*.jar /app/app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl --fail --silent --show-error --max-time 4 http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
