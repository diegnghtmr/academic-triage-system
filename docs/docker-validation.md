# Docker validation

This project now supports a minimal end-to-end Dockerized startup for local development.

## Start the stack

```bash
docker-compose up --build -d
```

Optional tools profile (Adminer):

```bash
docker-compose --profile tools up --build -d
```

## What runs

- `mariadb` — MariaDB 11 with a named Docker volume
- `app` — Spring Boot `bootstrap` module packaged as a container image
- `adminer` — optional DB GUI behind the `tools` profile

## Smoke check

```bash
./scripts/docker-smoke.sh
```

What it validates:

1. `/actuator/health`
2. `/swagger-ui.html`
3. user registration
4. login + JWT issuance
5. catalog discovery
6. request creation + request listing

## Persistence check

```bash
./scripts/docker-persistence-check.sh
```

What it validates:

1. runs the smoke check
2. restarts `mariadb` and `app`
3. waits for app health again
4. queries MariaDB directly to confirm the created request survived restart via the named volume

## Hardening notes

- Base config no longer allows Spring Boot's default in-memory user autoconfiguration.
- `adminer` is now opt-in via `--profile tools`.
- Compose uses the Docker network hostname `mariadb`, never `localhost`, for the app DB connection.
- The app healthcheck uses `/actuator/health` and waits for the service to be truly ready.

## Known non-blocking warnings

- Flyway warns that MariaDB 11.8 is newer than the latest MariaDB version it has officially tested.
- Hibernate no longer requires an explicit `MariaDBDialect`; the project now relies on auto-detection.
