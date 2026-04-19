# Idempotency & Concurrency: Cierre Técnico Final (Batch 5)

Este documento consolida el alcance, decisiones arquitectónicas, semántica HTTP y estado operativo final de la iniciativa de Idempotencia y Concurrencia para el Academic Triage System Backend.

## 1. Alcance Final Cubierto

La capacidad de idempotencia y control de concurrencia está desplegada y activa cubriendo las siguientes familias de operaciones:

- **Operaciones de Creación y Modificación (POST/PATCH):**
  - Protegidas con el header `Idempotency-Key`.
  - Evitan la duplicación de creación de registros (ej. requests de triage, reglas de negocio) y efectos colaterales de notificaciones, asegurando que un reintento devuelva la misma respuesta generada por el primer request procesado.

- **Operaciones de Actualización Estricta (PUT/DELETE):**
  - Protegidas mediante Optimistic Locking y el uso de cabeceras `ETag` (devuelta en los GET) e `If-Match` (requerida en las actualizaciones).
  - Evitan que un usuario sobrescriba los cambios recientes de otro usuario.

- **Generación de Resúmenes de IA (GET `/api/v1/ai/summarize/{requestId}`):**
  - Implementa caché de respuestas asociando el `requestId` con la versión de los datos (ETag).
  - Un mismo requerimiento con los mismos datos devuelve instantáneamente el resumen generado previamente (reutilización de caché).

- **Exclusiones Explícitas:**
  - El flujo de Login/Auth queda **fuera** de la validación estricta de idempotencia funcional, ya que los tokens generados son por naturaleza efímeros y regenerar un token repetidas veces por reintentos no corrompe el estado del negocio.

## 2. Semántica HTTP Final

El sistema responde de manera estandarizada ante los diferentes escenarios de concurrencia e idempotencia:

- `400 Bad Request`: Si falta el header requerido `Idempotency-Key` en endpoints configurados para validarla.
- `409 Conflict`: Si el mismo request (misma clave) se encuentra en vuelo (*outstanding* / *in-progress*).
- `412 Precondition Failed`: Si hay un mismatch de `ETag` al intentar usar `If-Match` con datos obsoletos (Stale data).
- `422 Unprocessable Entity`: Si hay un mismatch de *fingerprint*. Significa que el cliente reutilizó una clave de idempotencia existente pero cambiando el payload (body, path, headers críticos).
- `428 Precondition Required`: Si la actualización requiere `If-Match` y el cliente omitió la cabecera.
- `2xx / 4xx / 5xx (Replay)`: Si la petición se reintenta tras finalizar con éxito, se devuelve el estado exacto original incluyendo un header inyectado `Idempotency-Status: replayed`. Para requests frescos, es `Idempotency-Status: fresh`.

## 3. Política TTL y Limpieza (Cleanup)

- **Default TTL:** La mayoría de las claves retienen su valor durante un tiempo definido globalmente (generalmente horas o días, según la propiedad `triage.idempotency.ttl`).
- **AI / Operaciones pesadas (`ai:*`):** Suelen tener políticas diferentes o más permisivas si representan resúmenes de solo lectura pero pesados de recalcular.
- **Cleanup Schedule:** Existe un cron (`IdempotencyCleanupScheduler`) y un servicio dedicado que ejecuta la limpieza de claves expiradas.
- **Batch Size:** La purga se procesa en lotes para no bloquear transacciones del negocio o agotar recursos de base de datos.

## 4. Estrategia de Concurrencia

- **Optimistic Locking:** Utilizado en recursos administrativos y catálogos (`@Version`, `If-Match`) para proteger entidades contra actualizaciones concurrentes desde clientes distribuidos, favoreciendo la lectura sin bloqueos.
- **Pessimistic / Selective Locking:** Reservado para escenarios de mutación crítica, específicamente el aggregate `AcademicRequest` y la reclamación (`claim`) de una `Idempotency-Key` a nivel de base de datos con `FOR UPDATE` (si aplica el fallback).
- **Por qué no es global el Pessimistic Locking:** Afectaría negativamente el throughput del sistema, degradando el rendimiento en flujos de solo lectura que son el 90% del tráfico general.

## 5. Riesgos Aceptados y Gaps Pendientes

Estas omisiones son intencionales en esta fase y requieren iniciativas futuras:

- **Distributed Tracing & Correlation ID:** Aunque la entidad `idempotency_keys` en la BD (desde V25) soporta `correlation_id`, actualmente no se extrae automáticamente del MDC o de un filtro de trazabilidad central (ej. `X-Correlation-Id`), porque implementar trazabilidad distribuida excede este Batch. Pendiente para una iniciativa de Observabilidad Full.
- **Resource Tracking (resource_type / resource_id):** Similar al punto anterior, aunque el esquema soporta los campos `resource_type` y `resource_id`, inyectar esta metainformación de negocio limpio requeriría alterar transversalmente la firma del `IdempotencyExecutor` e introducir un componente acoplado al modelo en una capa de infraestructura genérica. Queda como extensión para analítica avanzada.
- **Arquitectura Event-Driven (Kafka / Outbox):** No se introdujo el patrón Transaccional Outbox, ya que el enfoque local en BD es suficiente para los requisitos actuales.
