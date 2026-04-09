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

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
