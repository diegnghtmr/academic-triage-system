# Academic Triage System Backend

Backend service for managing the full lifecycle of academic requests at Universidad del Quindío — from intake and classification to prioritization, assignment, follow-up, and closure.

This project is intentionally built as a **domain-first, hexagonal backend**. The business core remains independent from frameworks, persistence, and delivery concerns, while Spring Boot, JPA, security, and AI stay at the outer layers where they belong.

---

## Table of Contents

- [Why this project exists](#why-this-project-exists)
- [Core features](#core-features)
- [Design philosophy](#design-philosophy)
- [Architecture overview](#architecture-overview)
- [Technology stack](#technology-stack)
- [Repository structure](#repository-structure)
- [Getting started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run locally](#run-locally)
  - [Run with Docker](#run-with-docker)
- [Environment variables](#environment-variables)
- [Optional AI integration](#optional-ai-integration)
- [Testing](#testing)
- [API documentation](#api-documentation)
- [Security model](#security-model)
- [Operational validation](#operational-validation)
- [Project status](#project-status)

---

## Why this project exists

Academic service desks tend to fail for the same reasons: unclear intake, inconsistent categorization, manual prioritization, weak traceability, and no reliable operational visibility.

This backend addresses those problems by providing a structured workflow for academic requests with:

- explicit request states
- domain-enforced transitions
- auditable history entries
- role-based access control
- reporting and dashboard metrics
- optional AI assistance for classification and summarization

The result is a backend that is both **operationally useful** and **architecturally disciplined**.

---

## Core features

- **Academic request lifecycle management**
  - register
  - classify
  - prioritize
  - assign
  - attend
  - close
  - cancel
  - reject
- **Rich domain model** with enforced business rules and immutable terminal states
- **JWT authentication** with role-based authorization (`ADMIN`, `STAFF`, `STUDENT`)
- **Catalog management** for request types and origin channels
- **Business rule management** for priority decisions
- **User management** for staff and administrators
- **Dashboard/reporting endpoints** (ADMIN-only) for operational metrics
- **OpenAPI / Swagger UI** for interactive API exploration
- **Optional AI support** through Spring AI and OpenAI-compatible providers
- **Dockerized local stack** with MariaDB, application container, and optional Adminer

---

## Design philosophy

This codebase follows a few non-negotiable principles:

### 1. Domain first

Business rules live in the domain model, not in controllers or repositories.

### 2. Hexagonal architecture over framework-driven design

The application core defines ports. Infrastructure implements adapters. Spring is an implementation detail, not the center of the system.

### 3. Explicit boundaries

The project is split into modules to enforce architectural intent:

- `domain` — pure business logic
- `application` — use cases and ports
- `infrastructure` — REST, persistence, security, AI adapters
- `bootstrap` — runtime entrypoint and application configuration

### 4. Optional AI, never mandatory AI

The system must remain fully functional without an AI provider. AI enhances workflows; it does not own them.

### 5. Testable by design

The project uses unit tests, application-level tests, adapter tests, architecture tests, and runtime smoke checks to keep the system reliable.

---

## Architecture overview

```text
HTTP / Docker / Database / AI Provider
            │
            ▼
      infrastructure
   (REST, JPA, JWT, AI)
            │
            ▼
       application
   (use cases and ports)
            │
            ▼
          domain
  (entities, value objects,
   rules, transitions, events)
```

### Request lifecycle

```text
REGISTERED → CLASSIFIED → IN_PROGRESS → ATTENDED → CLOSED
      │             │
      ├─────────────┴──→ CANCELLED
      └────────────────→ REJECTED
```

### Architectural consequences

- domain and application remain framework-free
- persistence never leaks into the domain
- controllers depend on use cases, not on repositories
- AI is exposed through an application port and implemented by an infrastructure adapter
- architecture rules are enforced with ArchUnit tests

---

## Technology stack

| Category            | Technology                                                    |
| ------------------- | ------------------------------------------------------------- |
| Language            | Java 21                                                       |
| Framework           | Spring Boot 3.5.9                                             |
| Build               | Gradle 8 (Kotlin DSL)                                         |
| Database            | MariaDB 11                                                    |
| ORM                 | Spring Data JPA / Hibernate 6                                 |
| Database migrations | Flyway 10                                                     |
| Security            | Spring Security + JWT (`jjwt`)                                |
| Mapping             | MapStruct 1.6                                                 |
| AI                  | Spring AI 1.1.4                                               |
| API docs            | Springdoc OpenAPI 2.8                                         |
| Testing             | JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit, WireMock |
| Containers          | Docker / Docker Compose                                       |

---

## Repository structure

```text
academic-triage-system/
├── domain/           # Pure domain model and business rules
├── application/      # Use cases, commands, queries, ports
├── infrastructure/   # REST, persistence, security, AI adapters
├── bootstrap/        # Spring Boot entrypoint and configuration
├── docs/             # OpenAPI contract and diagrams
├── scripts/          # Docker smoke and persistence validation
├── Dockerfile        # Multi-stage app container build
└── docker-compose.yml
```

Useful documents:

- `AGENTS.md` — repository blueprint and engineering rules
- `docs/openapi-academic-triage.yaml` — API contract source of truth
- `docs/diagram_state.png` — state machine diagram
- `docs/diagram_classes.jpeg` / `docs/ER.jpeg` / `docs/MER.jpeg` — model diagrams

---

## Getting started

### Prerequisites

Install the following before running the project:

- Java 21
- Docker + Docker Compose
- Git

Optional but useful:

- `curl`
- `python` or `python3`
- `shellcheck`

---

### Run locally

1. **Clone the repository**

```bash
git clone <repo-url>
cd academic-triage-system
```

2. **Create a local environment file**

Use `.env.example` as the starting point.

3. **Start MariaDB**

```bash
docker-compose up -d mariadb
```

4. **Run the application**

Activa el perfil `dev` para desarrollo local: carga documentación, secret JWT de desarrollo en `application-dev.yml` y la segunda ruta Flyway `classpath:db/migration-dev`, que **solo en local** vuelve a crear el usuario administrador de prueba documentado abajo y además carga un dataset demo rico para dashboards, reportes y flujos multi-rol.

```bash
set -a
source .env
set +a
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootstrap:bootRun
```

Sin perfil `dev`, las migraciones compartidas (`db/migration`) **no** insertan ese admin: deberás registrarte vía API u operar otro usuario con privilegios.

5. **Open Swagger UI**

- `http://localhost:8080/swagger-ui.html` (con `APP_DOCS_ENABLED=true` o perfil `dev`, donde la documentación viene habilitada)

6. **Check health**

```bash
curl http://localhost:8080/actuator/health
```

---

### Run with Docker

Bring up the full local stack:

```bash
docker-compose up --build -d
```

El servicio `app` usa por defecto `SPRING_PROFILES_ACTIVE=dev`, así que Flyway aplica también `db/migration-dev` y quedan disponibles el **administrador local de desarrollo** (`admin` / `admin123`) y un dataset demo local con usuarios `STAFF` / `STUDENT`, catálogos adicionales, reglas de negocio y solicitudes en varios estados. Eso no forma parte del baseline “solo `db/migration`”: no lo habilités en entornos compartidos.

Credenciales locales de demo:

- `admin` / `admin123`
- usuarios demo (`staff_registro`, `staff_admisiones`, `staff_financiero`, `staff_bienestar`, `staff_homologa`, `ana_martinez`, `juan_perez`, etc.) / `admin123`

Si ya tenías una imagen previa levantada, volvés a correr `docker-compose up --build -d` y Flyway aplica la migración dev pendiente para alinear esas credenciales locales.

Check container status:

```bash
docker-compose ps
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI:

```bash
curl -I http://localhost:8080/swagger-ui.html
```

Optional Adminer profile:

```bash
docker-compose --profile tools up --build -d
```

Adminer will be available at:

- `http://localhost:8081`

---

## Environment variables

The application is driven by environment variables both locally and in Docker.

| Variable                              | Purpose                                    |
| ------------------------------------- | ------------------------------------------ |
| `DB_URL`                              | MariaDB JDBC URL                           |
| `DB_USER`                             | Database username                          |
| `DB_PASS`                             | Database password                          |
| `DB_ROOT_PASS`                        | MariaDB root password                      |
| `JWT_SECRET`                          | JWT signing key                            |
| `JWT_EXPIRATION_MS`                   | JWT expiration in milliseconds             |
| `SPRING_PROFILES_ACTIVE`              | Spring profile (`dev`, `test`, etc.)       |
| `APP_DOCS_ENABLED`                    | Enables Swagger/OpenAPI endpoints          |
| `APP_DOCS_PUBLIC_ENABLED`             | Makes docs public instead of admin-only    |
| `APP_AI_PROVIDER`                     | AI provider switch (`none`, `openai`)      |
| `SPRING_AI_MODEL_CHAT`                | Spring AI chat provider (`openai`, `none`) |
| `SPRING_AI_OPENAI_API_KEY`            | API key for OpenAI-compatible providers    |
| `SPRING_AI_OPENAI_BASE_URL`           | Base URL for OpenAI-compatible providers   |
| `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL` | Model name used by Spring AI chat          |

> **Important:** do not commit real secrets. Keep `.env` local only.

---

## Optional AI integration

AI is optional by design.

If AI is disabled or no provider is configured correctly, the application still works and the AI endpoints return a controlled `503 Service Unavailable` response.

### Current provider model

The project uses Spring AI with the OpenAI-compatible stack, which means you can plug in providers such as:

- OpenAI
- OpenRouter
- Groq
- other OpenAI-compatible endpoints

### Example: OpenRouter

```env
APP_AI_PROVIDER=openai
SPRING_AI_MODEL_CHAT=openai
SPRING_AI_OPENAI_API_KEY=<your-openrouter-key>
SPRING_AI_OPENAI_BASE_URL=https://openrouter.ai/api
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=openrouter/free
```

### AI endpoints

- `POST /api/v1/ai/suggest-classification`
- `GET /api/v1/ai/summarize/{requestId}`

### Manual AI validation flow

1. Login as a `STAFF` user
2. Call `suggest-classification`
3. Create a real request
4. Call `summarize/{requestId}`

This flow has been validated manually in Docker using OpenRouter.

---

## Idempotency & Concurrency Quick Guide

This backend implements strict idempotency and concurrency controls to prevent duplicate actions and lost updates.

- **POST / PATCH (Create & Prioritize):** Send an `Idempotency-Key` header (UUID recommended). If you send the same key with the same payload, you will get the exact same response (`Idempotency-Status: replayed`) without creating duplicates. If you change the payload, you get a `422`.
- **PUT / DELETE (Updates):** Fetch the resource first to get the `ETag` header. Send that value back in the `If-Match` header to update. If someone else updated the resource in the meantime, you will get a `412 Precondition Failed`.
- **AI Cache:** AI summary generation is cached based on the data version (`ETag`). Re-requesting the same summary is instant.

Detailed request/response contracts live in [docs/openapi-academic-triage.yaml](docs/openapi-academic-triage.yaml). Operational validation examples are available in `scripts/smoke-concurrency-idempotency.sh` and `scripts/audit-idempotency.sql`.

---

## Testing

### Run the full test suite

```bash
./gradlew test
```

### Coverage reporting

Generate an aggregate JaCoCo report for all modules:

```bash
./gradlew jacocoRootReport
```

Reports are available at:

- **Aggregate (Root):** `build/reports/jacoco/root/html/index.html` (HTML) and `build/reports/jacoco/root/jacocoRootReport.xml` (XML)
- **Per-module:** `{module}/build/reports/jacoco/test/html/index.html`

> **Note:** This change adds reporting only — no coverage thresholds or CI enforcement are applied at this stage.

### Run module-specific tests

```bash
./gradlew domain:test
./gradlew application:test
./gradlew infrastructure:test
./gradlew bootstrap:test
```

### Architecture tests

```bash
./gradlew infrastructure:test --tests "*ArchitectureTest*"
```

### Docker smoke validation

```bash
./scripts/docker-smoke.sh
```

### Docker persistence validation

```bash
./scripts/docker-persistence-check.sh
```

What the Docker scripts validate:

- app health
- Swagger availability
- register + login
- catalog access
- request creation
- request listing
- persistence after restarting `app` and `mariadb`

---

## API documentation

### Swagger UI

- `http://localhost:8080/swagger-ui.html`

### OpenAPI contract

- `docs/openapi-academic-triage.yaml`

The OpenAPI file is treated as the contract source of truth for the backend surface.

---

## Security model

Authentication is JWT-based and stateless.

### Roles

- `ADMIN`
- `STAFF`
- `STUDENT`

### Example permissions

| Endpoint area               | STUDENT | STAFF |         ADMIN |
| --------------------------- | ------: | ----: | ------------: |
| Register / login            |      ✅ |    ✅ |            ✅ |
| Create request              |      ✅ |    ✅ |            ❌ |
| Request operational actions |      ❌ |    ✅ | role-specific |
| AI suggest classification   |      ❌ |    ✅ |            ❌ |
| AI summarize                |      ❌ |    ✅ |            ✅ |
| Catalog management          |      ❌ |    ❌ |            ✅ |
| User management             |      ❌ |    ❌ |            ✅ |
| Dashboard / report metrics  |      ❌ |    ❌ |            ✅ |

The application also excludes Spring Boot’s default generated user configuration, relying instead on the project’s own JWT-based security model.

---

## Operational validation

The backend has been validated across the following layers:

- full Gradle test suite
- architecture enforcement via ArchUnit
- local runtime startup against MariaDB
- Dockerized startup (`docker-compose up --build -d`)
- Docker smoke and persistence scripts
- manual end-to-end AI validation with OpenRouter

Validated runtime flows include:

- authentication
- request creation
- request retrieval
- Docker persistence after restart
- AI classification suggestion
- AI summary generation

---

## Project status

This repository is currently in a **fully runnable and manually validated backend state**.

What is already working:

- modular hexagonal architecture
- JWT security
- Dockerized local stack
- reporting, catalogs, users, requests
- optional AI with OpenAI-compatible providers
- smoke and persistence validation scripts

---

If you are reviewing or extending this project, start with:

1. `docs/openapi-academic-triage.yaml`
2. `docker-compose.yml`
3. `README.md`

That path gives you the architecture, the contract, the runtime model, and the developer workflow in the right order.
