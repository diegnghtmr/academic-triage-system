# Academic Triage backend reference

## Design and module boundaries

`domain` owns requests, states, value objects, events, priority rules, and allowed transitions. `application` coordinates use cases through ports. The core does not depend on Spring or JPA. `infrastructure` implements REST delivery, persistence, JWT security, and the optional AI adapter; controllers invoke application use cases rather than repositories. `bootstrap` wires and configures the runtime. The architecture tests enforce the inward dependencies. The [state diagram](diagram_state.png), [class diagram](diagram_classes.jpeg), and [database diagrams](ER.jpeg) ([relational model](MER.jpeg)) provide additional context; use the current implementation if a diagram differs.

| From | Allowed next states |
| --- | --- |
| `REGISTERED` | `CLASSIFIED`, `CANCELLED`, `REJECTED` |
| `CLASSIFIED` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `ATTENDED` |
| `ATTENDED` | `CLOSED` |
| `CLOSED`, `CANCELLED`, `REJECTED` | None |

An allowed transition still requires authorization and any use-case preconditions. Assignment and prioritization are operations, not extra states. A priority suggestion is read-only and does not call AI.

## API documentation and security

Use the [OpenAPI contract](openapi-academic-triage.yaml) for paths, request bodies, headers, and responses. With docs enabled, Swagger UI is at `/swagger-ui.html` and the generated description is at `/api-docs`. `APP_DOCS_ENABLED=true` enables the routes for `ADMIN`; `APP_DOCS_PUBLIC_ENABLED=true` also permits unauthenticated access. Local Compose enables both flags by default. The [Postman collection](postman-academic-triage-system.collection.json) gives examples for a disposable local stack.

Spring Security is stateless. The application provides its own JWT filter and BCrypt password verification instead of Spring Boot's generated user. Registration, login, and the health probe are public; protected API calls use `Authorization: Bearer <token>`. Registration is not a way to obtain an administrator account.

| Operation | Permission |
| --- | --- |
| Create a request | `STUDENT` or `STAFF` |
| Read cases and history | Authenticated; students can read only their own requests |
| Classify, prioritize, assign, attend, close | `STAFF` |
| Cancel | `STAFF`, `ADMIN`, or the owning `STUDENT`; state constraints still apply |
| Reject | `ADMIN`; state constraints still apply |
| Read catalogs | Authenticated |
| Write catalogs and business rules; administer users; view reports | `ADMIN` |
| Read business rules | `ADMIN`, `STAFF` |
| Ask AI for classification | `STAFF` |
| Ask AI for a summary | `ADMIN`, `STAFF` |

`APP_CORS_ALLOWED_ORIGINS` is a comma-separated allow-list. The configured CORS request headers include `Authorization`, `Content-Type`, `Idempotency-Key`, and `If-Match`; only `Authorization` is exposed as a response header. For browser integrations that need `ETag`, use a same-origin proxy or explicitly arrange header exposure.

## Idempotency and concurrent updates

- **Create and transition operations:** Send `Idempotency-Key` where required by OpenAPI; a UUID is suitable. The same key and request fingerprint replay the stored response without another mutation (`Idempotency-Status: replayed`). A changed body with that key gets `422`. An operation still underway can return `409`; an absent required key gets `400`. These rules apply to designated endpoints, not to every request with the same HTTP verb.
- **Administrative edits:** Fetch the specific resource, read its strong `ETag`, and send it in `If-Match` on subsequent writes that require it. A stale precondition gets `412`; an omitted one gets `428`. Re-read the resource and reconcile the edit before retrying. Use-case transactions also lock request rows so workflow mutations do not act on the same stale state.
- **AI cache:** A summary is cached by request ID and request version. Repeating the read for an unchanged request can use that cache; this neither guarantees a particular latency nor means the summary response supplies an HTTP `ETag`.

See [the concurrency smoke script](../scripts/smoke-concurrency-idempotency.sh) and [the idempotency audit queries](../scripts/audit-idempotency.sql) for local validation. The script needs the development stack, `curl`, and `jq`; its AI path also needs an eligible request and a configured provider for a successful live result. It creates catalog data; do not target production.

## Optional AI provider configuration

The workflow works with AI disabled (`APP_AI_PROVIDER=none`). Without an available provider, AI endpoints return a controlled `503 Service Unavailable`. Spring AI uses an OpenAI-compatible chat endpoint; providers such as OpenAI, OpenRouter, or Groq require their own compatible URL, model, and key. Supply credentials privately to the process and use synthetic request text when testing a third-party endpoint.

For an OpenRouter configuration, set the following values in the API environment, selecting a model available to your account:

```dotenv
APP_AI_PROVIDER=openai
SPRING_AI_MODEL_CHAT=openai
SPRING_AI_OPENAI_API_KEY=<your-openrouter-api-key>
SPRING_AI_OPENAI_BASE_URL=https://openrouter.ai/api
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=<your-openrouter-model>
```

To validate locally, start the `dev` stack with that configuration. Sign in as `STAFF`, submit a fictional request description to `POST /api/v1/ai/suggest-classification` with an idempotency key, then create a synthetic academic request and fetch `GET /api/v1/ai/summarize/{requestId}`. Repeat the summary read without changing the case to exercise the cache. Disable AI and verify the app still handles ordinary requests and returns `503` on an uncached AI path. These are runbook steps, not evidence of a new provider test.

## Build and runtime validation

Use JDK 21, the Gradle wrapper, and Docker for container-backed tests. `curl` and Python 3 are needed for the Docker smoke scripts. CI runs Spotless, tests, aggregate coverage generation, and `bootstrap:bootJar`. Domain and application tests exercise transitions and use cases; adapter tests exercise persistence and HTTP boundaries, including controlled AI responses. See [the Gradle version properties](../gradle.properties) and module build files for dependency pins.

```sh
./gradlew test
./gradlew domain:test application:test infrastructure:test bootstrap:test
./gradlew infrastructure:test --tests '*ArchitectureTest*'
./gradlew jacocoRootReport
```

The JaCoCo root outputs are `build/reports/jacoco/root/html/index.html` and `build/reports/jacoco/root/jacocoRootReport.xml`. Individual modules produce `build/reports/jacoco/test/html/index.html` under their module roots. This build reports coverage without enforcing a threshold.

For an initialized **disposable** Compose stack using the development profile:

```sh
./scripts/docker-smoke.sh
./scripts/docker-persistence-check.sh
```

The smoke script checks health, Swagger, registration, login (including the legacy `username` alias), catalogs, and request creation/listing. The persistence script creates a test request, restarts the database and app containers, then checks that the request is still in the MariaDB-backed stack. These checks change local data and can interrupt service. Rebuild with `docker compose up --build -d` to apply pending Flyway migrations when updating the local stack. The shared migrations do not provision the development administrator or demo cases. With `dev`, the extra Flyway location supplies the disposable demo dataset for staff, student, and administrator flows. With a non-development profile, provision administrator access separately. Never reuse demo accounts outside local development.
