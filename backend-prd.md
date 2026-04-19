# PRD del backend

## 1. Propósito del documento

Este documento define el alcance, las decisiones de producto y las condiciones de implementación del backend del Sistema de Triage y Gestión de Solicitudes Académicas. Se basa en la guía maestra del curso, en `AGENTS.md`, en `docs/openapi-academic-triage.yaml`, en los diagramas del repositorio y en el estado real del código.

El objetivo no es describir una idea abstracta. El objetivo es dejar una guía ejecutable para construir el backend con criterios consistentes de negocio, arquitectura, seguridad, persistencia y pruebas.

## 2. Problema

El programa académico procesa solicitudes por múltiples canales sin un flujo unificado ni trazabilidad suficiente. Esa dispersión genera tres fallas operativas:

1. Registro inconsistente de solicitudes.
2. Priorización informal y dependiente de criterio manual no documentado.
3. Dificultad para saber quién hizo qué, cuándo lo hizo y en qué estado se encuentra cada caso.

El backend debe resolver ese problema central. No se trata solo de exponer endpoints. Debe establecer un modelo transaccional coherente para el ciclo de vida de la solicitud, garantizar trazabilidad y sostener control de acceso por rol.

## 3. Objetivo del producto

Construir un backend REST en Java 21 y Spring Boot, con arquitectura hexagonal, capaz de:

- registrar solicitudes académicas desde distintos canales;
- clasificar, priorizar y asignar casos según reglas de negocio;
- gestionar el ciclo de vida completo hasta su cierre;
- mantener historial auditable por solicitud;
- restringir operaciones por rol;
- exponer métricas operativas;
- soportar, de forma opcional y desacoplada, asistentes con IA sin comprometer la operación base.

## 4. Alcance del backend

### 4.1. Incluido en el alcance

- Autenticación con JWT.
- Registro y gestión de usuarios.
- Registro de solicitudes.
- Consulta paginada y detallada de solicitudes.
- Clasificación, priorización, asignación, atención, cierre, cancelación y rechazo.
- Historial auditable y notas internas.
- Catálogos de tipos de solicitud y canales de origen.
- Reglas de negocio de priorización.
- Reportes operativos para tablero.
- Integración opcional con IA a través de puertos y adaptadores.
- Persistencia con MariaDB y migraciones con Flyway.
- Documentación OpenAPI.
- Pruebas unitarias, de aplicación, de infraestructura y de arquitectura.

### 4.2. Fuera de alcance directo

- Interfaz de usuario Angular.
- Notificaciones por correo, SMS o WhatsApp.
- Carga masiva de archivos adjuntos.
- Integraciones institucionales externas distintas del proveedor opcional de IA.
- Módulos de analytics avanzados o inteligencia de negocio fuera del tablero operativo definido.

## 5. Usuarios y actores

### 5.1. `STUDENT`

- Registra solicitudes.
- Consulta sus solicitudes e historial.
- Cancela sus solicitudes cuando la política lo permite.

### 5.2. `STAFF`

- Clasifica, prioriza, asigna, atiende y cierra solicitudes.
- Consulta el universo operativo según permisos.
- Agrega notas internas.
- Consume capacidades opcionales de IA según contrato.

### 5.3. `ADMIN`

- Administra usuarios, catálogos y reglas de negocio.
- Rechaza solicitudes.
- Consulta reportes operativos.
- Gestiona la configuración estructural del sistema.

## 6. Principios del producto

1. La solicitud académica es el agregado principal del dominio.
2. Toda transición relevante debe quedar registrada en el historial.
3. Los estados terminales son inmutables.
4. La seguridad se aplica en backend, no en el frontend.
5. La IA asiste, no decide.
6. El contrato HTTP debe estar definido antes que los controladores.
7. La arquitectura debe impedir, no solo desaconsejar, el acoplamiento indebido.

## 7. Requerimientos funcionales consolidados

| RF | Requerimiento | Resolución en backend |
| --- | --- | --- |
| RF-01 | Registro estructurado de solicitudes | `POST /requests`, entidad `AcademicRequest`, historial inicial |
| RF-02 | Clasificación por tipo | `PATCH /requests/{id}/classify`, catálogo `request_types` |
| RF-03 | Priorización con reglas y justificación | `PATCH /requests/{id}/prioritize`, `BusinessRule`, `PriorityEngine` |
| RF-04 | Gestión coherente del ciclo de vida | `StateTransitionValidator`, endpoints de transición |
| RF-05 | Asignación de responsables activos | `PATCH /requests/{id}/assign`, validación de rol `STAFF` |
| RF-06 | Historial auditable | `request_history`, endpoints de consulta y nota interna |
| RF-07 | Consulta con filtros | `GET /requests`, especificaciones y paginación |
| RF-08 | Cierre controlado | `PATCH /requests/{id}/close`, observación obligatoria |
| RF-09 | Resumen IA | `GET /ai/summarize/{requestId}` |
| RF-10 | Sugerencia IA | `POST /ai/suggest-classification` |
| RF-11 | Funcionamiento sin IA | Adaptador `NoOp` o respuesta controlada sin dependencia dura |
| RF-12 | Exposición REST | OpenAPI 3.0.3 y controladores Spring |
| RF-13 | Autorización por rol | Spring Security + JWT + reglas por endpoint |

## 8. Alcance funcional de la primera versión operativa

La primera versión operativa del backend debe cubrir el flujo completo sin IA:

1. Registro de usuario.
2. Inicio de sesión.
3. Registro de solicitud.
4. Consulta de solicitudes.
5. Consulta de detalle e historial.
6. Clasificación.
7. Priorización.
8. Asignación.
9. Atención.
10. Cierre.
11. Cancelación.
12. Rechazo.
13. Administración de catálogos.
14. Administración básica de usuarios.
15. Reglas de negocio.

La IA puede incorporarse como incremento posterior siempre que el backend base permanezca funcional y que al menos una capacidad asistida quede lista para el proyecto final.

## 9. Arquitectura objetivo

### 9.1. Estilo arquitectónico

Arquitectura hexagonal con DDD táctico y cuatro módulos Gradle:

- `domain`: reglas de negocio puras.
- `application`: casos de uso y puertos.
- `infrastructure`: adaptadores REST, persistencia, seguridad, mapeo e integración externa.
- `bootstrap`: entrada de la aplicación y configuración transversal.

### 9.2. Regla de dependencia

- `infrastructure` puede depender de `application` y `domain`.
- `application` puede depender de `domain`.
- `domain` no puede depender de Spring, JPA, Lombok ni clases de infraestructura.

### 9.3. Decisiones técnicas obligatorias

- Java 21.
- Spring Boot 3.5.x.
- Gradle Kotlin DSL.
- MariaDB 11.
- JPA con Hibernate.
- Flyway para migraciones.
- JWT para autenticación.
- MapStruct en infraestructura.
- Lombok solo en infraestructura.
- ArchUnit para verificar arquitectura.

## 10. Estado funcional y técnico actual del repositorio

El repositorio ya incluye piezas valiosas del dominio y parte de la persistencia, pero no está listo como backend funcional completo. El estado actual se puede resumir así:

### 10.1. Ya presente

- Estructura multi módulo.
- Modelo de dominio base para solicitudes, historial, usuarios, reglas y enumeraciones.
- Validador de transiciones de estado.
- Entidades JPA principales.
- Aplicación Spring de arranque.
- Contrato OpenAPI completo.
- Diagramas de apoyo.

### 10.2. Aún faltante o parcial

- Puertos y casos de uso completos en `application`.
- Adaptadores de persistencia y repositorios listos para operación.
- Controladores REST, DTO y mapeadores del lado de entrada.
- Seguridad JWT completa.
- Configuración de beans por capa.
- Migraciones Flyway.
- Suite de pruebas por capa.
- Adaptadores reales de IA y adaptador `NoOp`.

Conclusión operativa: el PRD debe conducir una implementación incremental sobre una base ya iniciada, no sobre un lienzo en blanco.

## 11. Modelo de dominio y flujo de negocio

### 11.1. Entidad principal

`AcademicRequest` es el agregado raíz. Contiene la información operativa del caso y la colección de eventos de historial asociados.

### 11.2. Estados del ciclo de vida

Estados canónicos del backend:

- `REGISTERED`
- `CLASSIFIED`
- `IN_PROGRESS`
- `ATTENDED`
- `CLOSED`
- `CANCELLED`
- `REJECTED`

Etiquetas funcionales para interfaz o documentos narrativos:

- Registrada
- Clasificada
- En atención
- Atendida
- Cerrada
- Cancelada
- Rechazada

### 11.3. Reglas de transición

- `REGISTERED` → `CLASSIFIED`
- `CLASSIFIED` → `IN_PROGRESS`
- `IN_PROGRESS` → `ATTENDED`
- `ATTENDED` → `CLOSED`
- `REGISTERED` o `CLASSIFIED` → `CANCELLED`
- `REGISTERED` → `REJECTED`

### 11.4. Reglas de negocio obligatorias

- Toda transición crea un registro de historial.
- `close()` exige observación de cierre.
- `cancel()` exige motivo de cancelación.
- `reject()` exige motivo de rechazo.
- `assign()` exige usuario activo con rol `STAFF`.
- Los estados terminales no admiten nuevas transacciones de negocio.
- La priorización exige justificación.

## 12. Contrato API

### 12.1. Fuente de verdad

El contrato canónico es `docs/openapi-academic-triage.yaml`. Cualquier matriz interna o diagrama que contradiga ese archivo debe considerarse desactualizado.

### 12.2. Recursos principales

- `/auth`
- `/requests`
- `/users`
- `/catalogs/request-types`
- `/catalogs/origin-channels`
- `/business-rules`
- `/ai`
- `/reports`

### 12.3. Convenciones

- Base path: `/api/v1`
- JSON como formato exclusivo
- `ProblemDetail` o estructura equivalente para errores
- Paginación con `page`, `size`, `sort`
- Códigos `201`, `200`, `400`, `401`, `403`, `404` y `409` según corresponda

## 13. Persistencia y modelo de datos

### 13.1. Entidades núcleo

- `users`
- `academic_requests`
- `request_history`
- `request_types`
- `origin_channels`
- `business_rules`
- relación puente para reglas aplicadas a una solicitud

### 13.2. Consideraciones de diseño

- Índices para `status`, `priority`, `applicant_id`, `responsible_id` y fecha de registro.
- Claves foráneas explícitas entre solicitudes, usuarios, tipos y canales.
- Campo obligatorio `performed_by_id` en `request_history`.
- Columnas para `first_name`, `last_name`, `priority_justification`, `closing_observation`, `cancellation_reason`, `rejection_reason`, `attendance_observation` y `ai_suggested`.

### 13.3. Estrategia de migraciones

- Una migración por unidad de cambio significativa.
- Datos semilla mínimos para catálogos base y usuarios de prueba cuando aplique.
- Prohibido usar `ddl-auto: update` para evolución del esquema.

## 14. Seguridad

### 14.1. Requisitos

- Inicio de sesión con JWT.
- Autorización por rol en cada endpoint.
- Validación del propietario para acceso a recursos de estudiante.
- Contraseñas cifradas con BCrypt.

### 14.2. Observación de alineación

Los endpoints de IA y su matriz de permisos fueron alineados con el OpenAPI. El contrato vigente para el resumen queda restringido a `STAFF` y `ADMIN`, y la sugerencia de clasificación queda expuesta mediante `POST /ai/suggest-classification`.

## 15. IA opcional

### 15.1. Política de producto

La IA es soporte optativo. No puede convertirse en dependencia crítica del sistema.

### 15.2. Implementación esperada

- Puerto `AiAssistantPort` en `application`.
- Adaptador `SpringAiAssistantAdapter` en `infrastructure`.
- Adaptador alterno `NoOpAiAssistantAdapter` cuando no exista API key.

### 15.3. Recomendación de priorización

Si el equipo debe elegir una sola capacidad real de IA para cumplir la exigencia del proyecto final, conviene empezar por la sugerencia de clasificación y prioridad. Esa capacidad impacta más temprano el flujo y aporta valor directo al trabajo del personal.

## 16. Requisitos no funcionales

### 16.1. Calidad de código

- Dominio libre de framework.
- Constructores explícitos en dominio y aplicación.
- Comentarios solo cuando el código no sea evidente.
- Nombres coherentes con el lenguaje del dominio.

### 16.2. Rendimiento

- Consultas paginadas para listados.
- Índices en columnas de filtrado frecuente.
- Separación entre lectura de resumen y lectura de detalle cuando sea necesario.

### 16.3. Observabilidad

- Actuator habilitado.
- Logs de autenticación fallida, errores de transición y fallos de integración externa.
- Métricas básicas de salud y de acceso.

### 16.4. Testabilidad

- Pruebas de dominio rápidas y aisladas.
- Pruebas de aplicación con dobles de prueba.
- Pruebas de infraestructura con slices o Testcontainers.
- Prueba de arquitectura con ArchUnit.

## 17. Roadmap de implementación del backend

### Fase 1. Base arquitectónica y persistencia

- Completar módulos `application` e `infrastructure`.
- Definir puertos, comandos y servicios.
- Implementar migraciones Flyway.
- Completar mapeadores dominio ↔ JPA.
- Crear repositorios y adaptadores de persistencia.

Resultado esperado:

El backend compila, arranca y persiste entidades base.

### Fase 2. Flujo funcional principal

- Implementar autenticación y autorización.
- Implementar registro, consulta y detalle de solicitudes.
- Implementar clasificación, priorización, asignación, atención, cierre, cancelación y rechazo.
- Implementar historial y notas internas.

Resultado esperado:

El flujo completo sin IA es operable de punta a punta.

### Fase 3. Administración y endurecimiento

- Implementar administración de usuarios.
- Implementar catálogos y reglas de negocio.
- Implementar reportes.
- Agregar pruebas de arquitectura, persistencia y controladores.
- Afinar documentación OpenAPI y manejo de errores.

Resultado esperado:

El backend queda listo para integración con frontend y sustentación técnica.

### Fase 4. IA opcional

- Implementar adaptador real de IA.
- Habilitar al menos una capacidad asistida.
- Probar comportamiento degradado sin clave de API.

Resultado esperado:

Se cumple el valor agregado sin romper RF-11.

## 18. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Desalineación entre diagramas y contrato OpenAPI | Alto | Tomar el OpenAPI como contrato canónico y actualizar diagramas |
| Falta de definición de permisos en endpoints de IA | Medio | Congelar el alcance según OpenAPI y registrar cambio formal si se modifica |
| Implementación apurada de seguridad al final | Alto | Introducir JWT desde el inicio del backend, aunque el frontend se integre después |
| Dominio contaminado con framework | Alto | Validar con ArchUnit y revisión por capa |
| Dependencia rígida de IA | Alto | Implementar puerto + adaptador `NoOp` |
| Retrasos por falta de migraciones tempranas | Medio | Crear Flyway desde la primera fase |

## 19. Definition of Done del backend

El backend se considera terminado cuando cumple simultáneamente estas condiciones:

1. Todos los RF obligatorios sin IA están operativos.
2. El contrato OpenAPI describe fielmente el comportamiento implementado.
3. La arquitectura hexagonal se valida con pruebas automáticas.
4. Las migraciones recrean el esquema desde cero.
5. La seguridad JWT protege los endpoints correspondientes.
6. Existe cobertura automatizada de dominio, aplicación e infraestructura en flujos críticos.
7. La ausencia de configuración de IA no impide el arranque ni la operación base.
8. Las inconsistencias documentales identificadas quedan resueltas o registradas formalmente.

## 20. Criterio final de alineación

El backend debe construirse con esta jerarquía de decisión:

1. Reglas funcionales del curso.
2. Contrato OpenAPI como fuente de verdad externa.
3. `AGENTS.md` como fuente de verdad arquitectónica.
4. Código de dominio como comportamiento ejecutable.
5. Diagramas como artefactos que deben reflejar lo anterior.

Si el equipo no respeta esa jerarquía, terminará implementando un sistema distinto en cada documento. Eso no constituye una arquitectura consistente, sino un conjunto de artefactos inconexos.
