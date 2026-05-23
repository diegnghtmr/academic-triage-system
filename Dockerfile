# syntax=docker/dockerfile:1.7

FROM gradle:8.12-jdk21 AS builder
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY domain domain
COPY application application
COPY infrastructure infrastructure
COPY bootstrap bootstrap

RUN gradle bootstrap:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 spring

COPY --from=builder /workspace/bootstrap/build/libs/*.jar /app/app.jar

USER spring

EXPOSE 8080

# Default JVM tuning para entornos con poca RAM (e.g. Render free 512MB).
# Sobrescribible vía JAVA_TOOL_OPTIONS en el entorno.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss256k"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
