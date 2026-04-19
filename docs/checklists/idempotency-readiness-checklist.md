# Idempotency & Concurrency: Readiness Checklist

Este checklist sirve para validar que la iniciativa de Idempotencia y Concurrencia (Batch 5) está completamente operativa y lista para Release / QA.

## 1. Contratos y Documentación
- [x] **OpenAPI Actualizado**: Los endpoints relevantes especifican `Idempotency-Key` (POST/PATCH), `ETag` e `If-Match` (PUT/DELETE/GET).
- [x] **Documento Técnico Creado**: Existe `docs/idempotency-backend-final.md` con el alcance, semántica, TTL, concurrencia y riesgos aceptados.
- [x] **README Actualizado**: El doc principal del repositorio incluye una guía rápida o punteros de uso para los clientes (Idempotency & Concurrency Quick Guide).

## 2. Pruebas y Validación (Smoke Scripts)
- [x] **Smoke Tests Unificados**: Existe el script unificado `scripts/smoke-concurrency-idempotency.sh` que cubre explícitamente:
  - Idempotencia (replay exacto y ausencia de duplicados).
  - If-Match (rechazo `412` ante un `ETag` viejo y éxito con uno correcto).
  - AI Cache (validación de reutilización de resúmenes de IA vía ETag / Versioning).

## 3. Pruebas E2E / Integración (Uso Real)
- [x] **POST Create Retry**: Test automatizado confirma que un segundo request con igual clave no duplica el registro (retorna `replayed`).
- [x] **PATCH Prioritize Retry**: Test automatizado confirma la retención de estado sin duplicar efectos observables.
- [x] **Request Mismatch**: Test automatizado que envía mismo key distinto payload y retorna `422 Unprocessable Entity`.
- [x] **Outstanding / In-Progress**: Test o evidencia de rechazo con `409 Conflict` cuando hay un request paralelo ejecutándose.

## 4. Observabilidad y Métricas
- [x] **Dashboard / Métricas Documentadas**: Listadas en el README o runbooks.
- [x] **Replays Incrementan**: Tests validan que el counter `idempotency.replays.total` avanza.
- [x] **Mismatches Incrementan**: Tests validan que `idempotency.mismatches.total` avanza.
- [x] **Outstanding Incrementan**: Tests validan que `idempotency.outstanding.total` avanza.

## 5. Arquitectura y Mantenimiento
- [x] **Cleanup Activo**: El job `IdempotencyCleanupScheduler` está funcional y configurado.
- [x] **Locking Selectivo Validado**: Existe locking selectivo/pesimista para mutaciones críticas en el aggregate `AcademicRequest` y el claim de Idempotencia en BD, conviviendo con Optimistic Locking (`@Version` / `If-Match`) en recursos administrativos y catálogos.
- [x] **Auditoría Operativa**: Existe script `scripts/audit-idempotency.sql` (o doc) para auditar llaves expiradas, mismatches o replays excesivos.
- [x] **Riesgos Pendientes Explicitados**: Gaps menores (CorrelationId, MDC, Distributed Tracing) documentados en el informe final.
