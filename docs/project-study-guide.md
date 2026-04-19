# Guía de estudio completa — Academic Triage System Backend

> Este documento está pensado para que puedas **entender el proyecto de punta a punta**: problema de negocio, arquitectura, flujos, seguridad, datos, pruebas, runtime y estrategia de lectura. La idea NO es que leas código suelto como un robot, sino que entiendas el sistema como una construcción completa.

---

## 1. Qué resuelve este proyecto

El proyecto implementa el backend de un sistema para gestionar solicitudes académicas en la Universidad del Quindío.

Problemas que resuelve:

- intake desordenado de solicitudes
- categorización inconsistente
- priorización manual y subjetiva
- poca trazabilidad del proceso
- poca visibilidad operativa

El sistema modela el ciclo completo de una solicitud académica:

1. registro
2. clasificación
3. priorización
4. asignación
5. atención
6. cierre

Y además contempla caminos alternativos:

- cancelación
- rechazo

En términos de negocio, la pieza central es una **AcademicRequest** con estado, historial, responsable, prioridad y reglas aplicadas.

---

## 2. La idea arquitectónica principal

Este backend está construido como un sistema **hexagonal, domain-first y modular**.

La regla más importante es esta:

```text
bootstrap -> infrastructure -> application -> domain
```

Eso significa:

- **domain** no conoce Spring, JPA ni HTTP
- **application** coordina casos de uso y define puertos
- **infrastructure** implementa adaptadores concretos
- **bootstrap** arranca la app y carga configuración

### Qué gana el proyecto con esto

- reglas de negocio aisladas del framework
- tests más rápidos y más claros
- posibilidad de cambiar delivery/persistencia sin romper el núcleo
- arquitectura más mantenible a largo plazo

---

## 3. Estructura física del repositorio

```text
academic-triage-system/
├── domain/
├── application/
├── infrastructure/
├── bootstrap/
├── docs/
├── scripts/
├── Dockerfile
└── docker-compose.yml
```

### 3.1 domain/

Contiene el corazón del negocio.

Acá viven:

- entidades y agregados
- value objects
- enums de dominio
- servicios de dominio
- excepciones de negocio
- eventos de dominio

**Archivos clave para estudiar:**

- `domain/.../model/AcademicRequest.java`
- `domain/.../model/BusinessRule.java`
- `domain/.../model/User.java`
- `domain/.../service/PriorityEngine.java`
- `domain/.../service/StateTransitionValidator.java`
- `domain/.../enums/RequestStatus.java`
- `domain/.../enums/ConditionType.java`
- `domain/.../enums/Role.java`

### 3.2 application/

Contiene la lógica de orquestación.

Acá viven:

- input ports (`*UseCase`, `*Query`)
- output ports (`*Port`)
- command/query models
- servicios de aplicación

**Idea clave:** esta capa NO debería saber cómo se persiste algo ni cómo llega una request HTTP. Solo sabe coordinar el negocio.

**Archivos clave:**

- `application/.../service/request/CreateRequestService.java`
- `application/.../service/request/ListRequestsService.java`
- `application/.../service/auth/LoginService.java`
- `application/.../port/in/auth/AuthenticatedActor.java`

### 3.3 infrastructure/

Acá vive todo lo concreto:

- controllers REST
- DTOs
- mappers
- seguridad JWT
- adapters de persistencia JPA
- adapters de AI
- configuraciones Spring
- migraciones Flyway
- tests de integración y arquitectura

**Archivos clave:**

- `infrastructure/.../adapter/in/rest/RequestController.java`
- `infrastructure/.../adapter/in/rest/BusinessRuleController.java`
- `infrastructure/.../adapter/in/rest/AiAssistantController.java`
- `infrastructure/.../config/SecurityConfiguration.java`
- `infrastructure/.../config/AiConfiguration.java`
- `infrastructure/.../config/BeanConfiguration.java`
- `infrastructure/.../architecture/ArchitectureTest.java`

### 3.4 bootstrap/

Es la entrada de runtime.

Acá están:

- `Application.java`
- `application.yml`
- perfiles `dev`, `test`, `prod`

No debería contener lógica de negocio. Solo arranque y configuración.

---

## 4. Tecnología usada y por qué importa

### Stack principal

- **Java 21**
- **Spring Boot 3.5.9**
- **Gradle Kotlin DSL**
- **MariaDB 11**
- **Spring Data JPA / Hibernate 6**
- **Flyway**
- **Spring Security + JWT**
- **MapStruct**
- **Spring AI 1.1.4**
- **Springdoc OpenAPI**
- **JUnit 5 / AssertJ / Testcontainers / ArchUnit / WireMock**

### Qué tenés que entender de verdad

- Spring Boot es el runtime, **no el centro conceptual del sistema**
- JPA es una implementación de persistencia, **no el modelo de dominio**
- JWT resuelve autenticación/autorización stateless
- Flyway garantiza versionado de esquema y seeds
- ArchUnit sirve para vigilar que la arquitectura no se desarme con el tiempo

---

## 5. Modelo mental del sistema

Pensalo así:

```text
Cliente HTTP
   -> Controller REST
   -> Use Case (application)
   -> Domain model / Domain services
   -> Output ports
   -> Persistence / Security / AI adapters
   -> DB / JWT / proveedor AI
```

### Ejemplo concreto: crear solicitud

1. el cliente hace `POST /api/v1/requests`
2. `RequestController` recibe el DTO
3. `RequestRestMapper` lo transforma en command
4. `CreateRequestService` valida actor, catálogos y usuario solicitante
5. usa puertos para cargar request type y origin channel
6. crea una nueva solicitud vía `CreateRequestPort`
7. devuelve un `RequestSummary`
8. el controller responde el DTO REST

Eso es hexagonal bien hecho: el controller no habla directo con repositorios, y el dominio no sabe nada de HTTP.

---

## 6. El dominio que realmente manda

## 6.1 AcademicRequest

Es el agregado principal.

Responsabilidades:

- mantener el estado actual de la solicitud
- guardar prioridad y justificación
- guardar responsable
- registrar razones de cancelación/rechazo/cierre/atención
- mantener historial interno
- impedir transiciones inválidas

### Estado inicial

Cuando se crea una `AcademicRequest`:

- arranca en `REGISTERED`
- se crea automáticamente un `RequestHistory` con acción `REGISTERED`

### Transiciones principales

Desde `AcademicRequest.java` + `StateTransitionValidator.java`:

```text
REGISTERED -> CLASSIFIED -> IN_PROGRESS -> ATTENDED -> CLOSED
REGISTERED -> CANCELLED
REGISTERED -> REJECTED
CLASSIFIED -> CANCELLED
```

### Reglas importantes

- no se puede asignar si la solicitud no fue priorizada
- no se puede asignar a un usuario inactivo
- el responsable debe ser `STAFF`
- `close()` exige observación de cierre válida
- `cancel()` exige razón
- `reject()` exige razón
- no se pueden agregar notas internas a estados terminales

### Estados terminales

Los estados terminales son:

- `CLOSED`
- `CANCELLED`
- `REJECTED`

Una vez ahí, la solicitud ya no sigue fluyendo.

---

## 6.2 BusinessRule

Representa una regla que puede sugerir prioridad.

Campos conceptuales:

- nombre
- descripción
- tipo de condición
- valor de condición
- prioridad resultante
- activa / inactiva
- request type asociado opcional/obligatorio según el tipo

### Tipos de condición

`ConditionType` tiene hoy:

- `REQUEST_TYPE`
- `DEADLINE`
- `REQUEST_TYPE_AND_DEADLINE`

### Idea clave

La regla no solo se guarda: también se **canoniza y valida**.

Ejemplos:

- `REQUEST_TYPE` exige `requestTypeId`
- `DEADLINE` no admite `requestTypeId`
- los días deben ser numéricos y no negativos
- si el tipo es `REQUEST_TYPE`, el `conditionValue` debe coincidir con el ID del tipo de solicitud

Esto es FUNDAMENTAL: el sistema no deja que guardes basura semántica en reglas de negocio activas.

---

## 6.3 PriorityEngine

Es un servicio de dominio que evalúa reglas activas contra una solicitud.

Qué hace:

- filtra las reglas que matchean
- toma la prioridad más alta entre las que aplican
- si no matchea ninguna, devuelve `LOW`
- puede devolver también detalle de qué reglas aplicaron

Eso evita duplicar lógica de prioridad en controllers o queries SQL.

---

## 6.4 User y roles

Roles del sistema:

- `ADMIN`
- `STAFF`
- `STUDENT`

Un detalle importante del dominio:

- un usuario admin puede registrar usuarios con rol explícito
- si no hay actor admin, el registro cae naturalmente a `STUDENT`

Esto evita que cualquiera se auto-registre como admin o staff.

---

## 7. Capa de aplicación: casos de uso y actores

La capa application orquesta el dominio.

## 7.1 AuthenticatedActor

La record `AuthenticatedActor` encapsula:

- `userId`
- `username`
- `role`

Eso permite que los casos de uso tomen decisiones con identidad y rol sin depender de Spring Security directamente.

## 7.2 Ejemplo: CreateRequestService

`CreateRequestService`:

- exige actor autenticado
- permite crear solicitudes solo a `STUDENT` y `STAFF`
- bloquea a `ADMIN` para intake
- valida que el usuario exista y esté activo
- valida que request type y origin channel existan y estén activos
- crea la solicitud con timestamp actual y `aiSuggested = false`

## 7.3 Ejemplo: ListRequestsService

`ListRequestsService` implementa una regla funcional MUY importante:

- si el actor es `STUDENT`, el filtro efectivo de requester se fuerza a su propio `userId`
- si es `STAFF` o `ADMIN`, puede usar el filtro que corresponda

Eso resuelve seguridad de acceso a nivel de caso de uso, no solo a nivel de endpoint.

---

## 8. Controllers REST: superficie pública del backend

El backend expone varias áreas REST.

## 8.1 AuthController

Base path: `/api/v1/auth`

Endpoints:

- `POST /register`
- `POST /login`

## 8.2 CatalogController

Base path: `/api/v1/catalogs`

Request types:

- `GET /request-types`
- `GET /request-types/{typeId}`
- `POST /request-types`
- `PUT /request-types/{typeId}`

Origin channels:

- `GET /origin-channels`
- `GET /origin-channels/{channelId}`
- `POST /origin-channels`
- `PUT /origin-channels/{channelId}`

## 8.3 BusinessRuleController

Base path: `/api/v1/business-rules`

- `GET /`
- `GET /{ruleId}`
- `POST /`
- `PUT /{ruleId}`
- `DELETE /{ruleId}`

El delete es lógico: desactiva la regla.

## 8.4 RequestController

Base path: `/api/v1/requests`

Operaciones:

- `POST /`
- `PATCH /{id}/classify`
- `PATCH /{id}/prioritize`
- `PATCH /{id}/assign`
- `PATCH /{id}/attend`
- `PATCH /{id}/close`
- `PATCH /{id}/cancel`
- `PATCH /{id}/reject`
- `GET /`
- `GET /{id}`
- `GET /{id}/priority-suggestion`
- `GET /{id}/history`
- `POST /{id}/history`

## 8.5 UserController

Base path: `/api/v1/users`

- `GET /`
- `GET /{id}`
- `PUT /{id}`

## 8.6 AiAssistantController

Base path: `/api/v1/ai`

- `POST /suggest-classification`
- `GET /summarize/{requestId}`

## 8.7 ReportController

Base path: `/api/v1/reports`

- `GET /dashboard`

---

## 9. Seguridad: cómo está pensada de verdad

## 9.1 Tipo de autenticación

La autenticación es:

- JWT
- stateless
- sin sesión de servidor

`SecurityConfiguration.java` configura:

- `BCryptPasswordEncoder`
- `JwtAuthenticationFilter`
- `JwtTokenAdapter`
- `SessionCreationPolicy.STATELESS`

## 9.2 Paths públicos

Se permiten sin token:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /actuator/health`

La documentación Swagger puede quedar:

- pública en `dev`
- restringida a admin fuera de `dev`
- o deshabilitada

## 9.3 Roles y permisos importantes

### Requests

- `STUDENT` y `STAFF` pueden crear requests
- `ADMIN` no crea requests
- operaciones operativas (`classify`, `prioritize`, `assign`, `attend`, `close`) están orientadas a staff
- `cancel` y `reject` tienen reglas específicas de negocio y/o rol

### Business rules

- listar/ver: `ADMIN` y `STAFF`
- crear/editar/desactivar: `ADMIN`

### AI

- `suggest-classification`: `STAFF`
- `summarize`: `STAFF` y `ADMIN`

### Reports

- `dashboard`: solo `ADMIN`

### Historial

Hay una regla fina en `RequestController`:

- un `STUDENT` solo puede ver el historial de solicitudes propias

Eso está implementado explícitamente, no asumido.

---

## 10. Persistencia y base de datos

El proyecto usa MariaDB con Flyway.

## 10.1 Qué gestiona Flyway

- creación de tablas
- constraints
- datos base
- migraciones correctivas
- seeds dev/test

## 10.2 Qué NO se hace

No se usa:

- `ddl-auto=create`
- `ddl-auto=update`

En `application.yml` está:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

Eso obliga a que el esquema venga de migraciones versionadas. Bien hecho.

## 10.3 Seeds importantes

### Baseline compartido

Incluye:

- request types base
- origin channels base
- business rules base

### Admin local

El admin local NO vive en el baseline final compartido.

Secuencia importante:

- `V20__remove_baseline_admin_seed.sql` lo elimina del baseline común
- `db/migration-dev` lo repone/alinea para `dev/test`
- `V22__align_local_admin_password.sql` asegura `admin / admin123`

### Idea conceptual

Esto separa bien:

- datos compartidos/productivos
- datos de conveniencia para desarrollo local

---

## 11. AI: por qué está y por qué NO rompe el sistema

La IA es opcional.

Eso no es marketing, está implementado así en `AiConfiguration.java`.

Si `app.ai.provider != openai` o no hay `ChatModel` disponible:

- se inyecta `NoOpAiAssistantAdapter`
- el sistema sigue funcionando
- los endpoints AI responden controladamente, típicamente con `503`

### Qué implica esto

- la IA mejora el sistema
- pero NO es dependencia obligatoria del core
- el backend conserva integridad incluso sin proveedor externo

Esto es una decisión arquitectónica excelente para evitar acoplamiento innecesario.

---

## 12. Runtime y perfiles

## 12.1 application.yml

Configuraciones importantes:

- puerto `8080`
- virtual threads habilitados
- datasource MariaDB
- Flyway activo
- OpenAPI configurable por flags
- AI deshabilitada por defecto

## 12.2 Perfil dev

`application-dev.yml` agrega:

- docs habilitadas
- logging más verboso
- `db/migration-dev`

Eso hace que localmente sea más fácil:

- ver Swagger
- usar el admin local
- depurar

## 12.3 Docker Compose

Levanta:

- `mariadb`
- `app`
- `adminer` opcional con profile `tools`

Detalle importante:

- `app` usa `SPRING_PROFILES_ACTIVE=dev` por defecto en Docker Compose

Por eso, en local Docker, el entorno viene listo para pruebas manuales y Postman.

---

## 13. Testing: cómo está defendido el backend

El proyecto tiene varias capas de pruebas.

## 13.1 Domain tests

Validan:

- reglas de entidades
- transiciones de estado
- priorización
- validación de invariantes

## 13.2 Application tests

Validan:

- casos de uso
- autorización funcional
- coordinación entre puertos
- filtros dependientes del actor

## 13.3 Infrastructure tests

Validan:

- controllers REST (`@WebMvcTest`)
- persistencia (`@DataJpaTest` + Testcontainers)
- seguridad
- migraciones

## 13.4 Architecture tests

`ArchitectureTest.java` hace cumplir:

- arquitectura onion/hexagonal
- dominio sin Spring/Jakarta/Lombok
- application sin Spring/Jakarta/Lombok
- REST adapters sin dependencia en out adapters
- output ports deben ser interfaces

## 13.5 Runtime validation

Además del test suite, el proyecto usa:

- Docker smoke script
- Docker persistence script
- colección Postman completa

Eso te da evidencia automática **y** evidencia end-to-end.

---

## 14. Cómo estudiar este proyecto sin perderte

Acá está el orden correcto. No empieces por donde “brilla”; empezá por donde **explica**.

## Fase 1 — visión global

Leé en este orden:

1. `README.md`
2. `docs/openapi-academic-triage.yaml`
3. `docs/diagram_state.png`
4. `docker-compose.yml`
5. `bootstrap/src/main/resources/application.yml`

Objetivo:

- entender qué expone el sistema
- cómo arranca
- qué actores y flujos existen

## Fase 2 — núcleo del negocio

Leé:

1. `AcademicRequest.java`
2. `BusinessRule.java`
3. `User.java`
4. `StateTransitionValidator.java`
5. `PriorityEngine.java`

Preguntas que tenés que poder responder:

- ¿qué es una solicitud y qué puede hacer?
- ¿quién decide si una transición es válida?
- ¿cómo se asigna prioridad?
- ¿cuándo una solicitud queda inmóvil?

## Fase 3 — casos de uso

Leé:

1. `CreateRequestService.java`
2. `ClassifyRequestService.java`
3. `PrioritizeRequestService.java`
4. `AssignRequestService.java`
5. `ListRequestsService.java`
6. `LoginService.java`
7. `RegisterService.java`

Preguntas clave:

- ¿qué valida el use case y qué valida el dominio?
- ¿qué sale por puertos?
- ¿qué restricciones dependen del actor?

## Fase 4 — adaptadores de entrada

Leé los controllers.

Objetivo:

- mapear endpoints al use case real
- entender DTOs y request/response boundaries
- ver dónde se usa `@PreAuthorize`

## Fase 5 — adaptadores de salida y configuración

Leé:

- adapters de persistencia
- entities JPA
- repositories Spring Data
- `BeanConfiguration.java`
- `SecurityConfiguration.java`
- `AiConfiguration.java`

Objetivo:

- entender cómo los puertos se enchufan a implementaciones concretas
- entender cómo Spring solamente cablea el sistema

## Fase 6 — pruebas

Leé:

- `ArchitectureTest.java`
- tests de request flow
- tests de migración
- tests de seguridad y controladores

Objetivo:

- entender qué decisiones del proyecto están protegidas por tests

---

## 15. Preguntas que tenés que poder contestar para decir “entendí el proyecto”

1. ¿Por qué `domain` no debe depender de Spring?
2. ¿Cuál es la diferencia entre `application` e `infrastructure`?
3. ¿Qué responsabilidades tiene `AcademicRequest` y cuáles NO?
4. ¿Cómo se evita que un estudiante vea solicitudes ajenas?
5. ¿Cómo se calcula la prioridad?
6. ¿Qué pasa si la IA está deshabilitada?
7. ¿Por qué Flyway es importante en este proyecto?
8. ¿Qué datos existen solo en `dev/test` y no en baseline compartido?
9. ¿Cómo se garantiza que la arquitectura no se degrade?
10. ¿Qué endpoint usarías para recorrer todo el flujo de una solicitud?

Si no podés responder eso, todavía no entendiste el sistema: solo leíste archivos.

---

## 16. Fortalezas técnicas del proyecto

- arquitectura modular clara
- dominio relativamente rico y no anémico
- buenas barreras entre capas
- seguridad JWT consistente
- IA opcional, no invasiva
- migraciones versionadas
- pruebas automáticas en varios niveles
- runtime local simple con Docker
- contrato OpenAPI explícito

---

## 17. Cosas a mirar críticamente si vas a extenderlo

- que nuevas reglas no empujen lógica al controller
- que nuevos adapters no contaminen `application` o `domain`
- que nuevos endpoints respeten el modelo de actor autenticado
- que no aparezcan `@Entity`, Lombok o Spring annotations dentro del dominio
- que las migraciones nuevas no editen historia vieja de Flyway
- que las reglas de negocio sigan vivas en el modelo, no en SQL ni en DTOs

---

## 18. Ruta corta de lectura recomendada

Si tenés solo una hora:

1. `README.md`
2. `docs/openapi-academic-triage.yaml`
3. `AcademicRequest.java`
4. `BusinessRule.java`
5. `CreateRequestService.java`
6. `RequestController.java`
7. `SecurityConfiguration.java`
8. `ArchitectureTest.java`

Si entendés bien esos 8 puntos, ya tenés armado el mapa mental del 80% del backend.

---

## 19. Ruta profunda de lectura recomendada

Si querés entenderlo de verdad, estudiá así:

### Día 1
- problema de negocio
- API contract
- flujo de estados

### Día 2
- dominio completo
- reglas de prioridad
- usuarios y roles

### Día 3
- casos de uso de requests
- auth
- business rules

### Día 4
- controllers
- mappers
- DTOs

### Día 5
- persistencia JPA
- migraciones Flyway
- configuración Spring

### Día 6
- testing strategy
- ArchUnit
- runtime Docker

Eso te deja en un nivel donde ya no “usás” el proyecto: lo podés explicar, criticar y extender.

---

## 20. Archivos que sí o sí tenés que tener abiertos mientras estudiás

- `README.md`
- `AGENTS.md`
- `docs/openapi-academic-triage.yaml`
- `domain/.../AcademicRequest.java`
- `domain/.../BusinessRule.java`
- `application/.../CreateRequestService.java`
- `application/.../ListRequestsService.java`
- `infrastructure/.../RequestController.java`
- `infrastructure/.../SecurityConfiguration.java`
- `infrastructure/.../BeanConfiguration.java`
- `infrastructure/.../ArchitectureTest.java`
- `bootstrap/src/main/resources/application.yml`
- `docker-compose.yml`

---

## 21. Cierre

Este backend está bueno para estudiar porque combina varias cosas importantes al mismo tiempo:

- arquitectura hexagonal real
- dominio con reglas explícitas
- seguridad JWT
- persistencia versionada
- testing serio
- integración opcional con AI

No es solo un CRUD con esteroides. Tiene decisiones de diseño que valen la pena entender.

Si lo estudiás bien, no solo entendés este proyecto: entendés una forma madura de construir backends Spring sin convertir Spring en tu modelo mental.

Y eso, hermano, es lo que realmente importa.
