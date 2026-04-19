# Dashboard de Observabilidad: Idempotencia

Este documento define los paneles, métricas, thresholds y alertas para monitorear la capa de idempotencia de `academic-triage-system`.

Todas las métricas son expuestas por Micrometer. En producción deben estar scrapeadas por Prometheus y visualizadas en Grafana. En staging puede usarse el endpoint `/actuator/metrics` directamente.

---

## Métricas disponibles

### Métricas de ejecución

| Métrica | Tipo | Descripción |
|---------|------|-------------|
| `idempotency.requests.total` | Counter | Total de requests que pasaron por el guard |
| `idempotency.replays.total` | Counter | Requests servidos desde cache (respuesta idempotente) |
| `idempotency.mismatches.total` | Counter | Requests con fingerprint mismatch (→ 422) |
| `idempotency.outstanding.total` | Counter | Requests rechazados por estar en vuelo (→ 409) |
| `idempotency.claim.latency` | Timer | Tiempo de claim (incluye lock y resolución de conflictos) |
| `idempotency.replay.latency` | Timer | Tiempo de servir un replay desde cache |

### Métricas de cleanup

| Métrica | Tipo | Descripción |
|---------|------|-------------|
| `idempotency.cleanup.runs.total` | Counter | Ejecuciones del cleanup job |
| `idempotency.cleanup.deleted.total` | Counter | Filas eliminadas en total por cleanup |
| `idempotency.cleanup.failures.total` | Counter | Ejecuciones del cleanup que terminaron en error |
| `idempotency.cleanup.duration` | Timer | Tiempo por ejecución de cleanup batch |

---

## Paneles recomendados (Grafana)

### Panel 1: Request rate y tipos de respuesta

```
# Tasa de requests idempotentes totales
rate(idempotency_requests_total[5m])

# Tasa de replays
rate(idempotency_replays_total[5m])

# Replay ratio (% de requests que son replays)
rate(idempotency_replays_total[5m]) / rate(idempotency_requests_total[5m]) * 100
```

**Tipo**: Time series. **Threshold normal**: replay ratio < 40%.

---

### Panel 2: Problemas de idempotencia

```
# Mismatches por minuto
rate(idempotency_mismatches_total[1m])

# Outstanding por minuto  
rate(idempotency_outstanding_total[1m])
```

**Tipo**: Stat o Time series. **Threshold de alerta**: > 5 mismatches/min o > 20 outstanding/min.

---

### Panel 3: Latencia de claim

```
# P95 de claim latency
histogram_quantile(0.95, rate(idempotency_claim_latency_seconds_bucket[5m]))

# P99 de claim latency
histogram_quantile(0.99, rate(idempotency_claim_latency_seconds_bucket[5m]))
```

**Threshold de alerta**: P95 > 500ms sugiere contención de locks en `idempotency_keys`.

---

### Panel 4: Cleanup job

```
# Filas eliminadas por cleanup en los últimos 30 minutos
increase(idempotency_cleanup_deleted_total[30m])

# Fallos de cleanup
rate(idempotency_cleanup_failures_total[30m])

# Duración del cleanup batch (P99)
histogram_quantile(0.99, rate(idempotency_cleanup_duration_seconds_bucket[30m]))
```

**Tipo**: Stat. **Threshold de alerta**: failures > 3 en 30 minutos, o cleanup sin actividad por > 1 hora.

---

### Panel 5: Estado de la tabla (query directa)

Estos no son métricas Prometheus — ejecutar directamente o como data source SQL:

```sql
-- Registros vivos vs expirados
SELECT
  status,
  CASE WHEN expires_at < NOW() THEN 'expired' ELSE 'active' END AS expiry_state,
  COUNT(*) AS count
FROM idempotency_keys
GROUP BY status, expiry_state;
```

---

## Alertas recomendadas

### CRITICAL — Outstanding spike

```yaml
alert: IdempotencyOutstandingSpike
expr: rate(idempotency_outstanding_total[5m]) > 20
for: 2m
summary: "Spike de 409 Outstanding en idempotencia"
description: "Más de 20 outstanding/min por 2 minutos. Puede indicar requests stuck o deadlocks."
```

### WARNING — Mismatch spike

```yaml
alert: IdempotencyMismatchSpike
expr: rate(idempotency_mismatches_total[5m]) > 5
for: 5m
summary: "Spike de fingerprint mismatches"
description: "Clientes generando keys incorrectamente o un proxy modificando el body."
```

### WARNING — Cleanup sin actividad

```yaml
alert: IdempotencyCleanupInactive
expr: increase(idempotency_cleanup_runs_total[1h]) == 0
for: 10m
summary: "El cleanup job no ha corrido en la última hora"
description: "IdempotencyCleanupScheduler no está ejecutando. Verificar que @EnableScheduling esté activo."
```

### WARNING — Cleanup failures sostenidas

```yaml
alert: IdempotencyCleanupFailures
expr: rate(idempotency_cleanup_failures_total[30m]) > 0
for: 30m
summary: "El cleanup de idempotencia está fallando repetidamente"
description: "Revisar logs de IdempotencyCleanupService. Posible problema de conectividad o timeout de DB."
```

### INFO — Replay ratio anómalo

```yaml
alert: IdempotencyReplayRatioAnomaly
expr: (rate(idempotency_replays_total[10m]) / rate(idempotency_requests_total[10m])) > 0.6
for: 10m
summary: "Ratio de replays muy alto (> 60%)"
description: "Clientes reintentan más de lo esperado. Puede ser comportamiento normal en recovery, o bug de retry logic."
```

---

## Configuración recomendada de Actuator

En `application.yml` o `application-prod.yml`, para exponer las métricas a Prometheus:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Setup operacional real (Batch 4A.1)

Esta sección reemplaza la guía teórica anterior. La dependencia está incluida y el endpoint es funcional desde la versión del batch 4A.1.

### Dependencia requerida

`bootstrap/build.gradle.kts`:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
runtimeOnly("io.micrometer:micrometer-registry-prometheus")
```

`micrometer-registry-prometheus` es `runtimeOnly` — Micrometer registra métricas en código; el bridge a Prometheus sólo se necesita en runtime.

### Configuración activa en `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name}
```

La etiqueta `application` se propaga automáticamente a todas las métricas como label Prometheus. Útil para filtrar en entornos multi-servicio.

### Endpoint real

```
GET http://localhost:8080/actuator/prometheus
Content-Type: text/plain; version=0.0.4; charset=utf-8
```

### Ejemplo mínimo de scrape config (Prometheus)

```yaml
scrape_configs:
  - job_name: 'academic-triage'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    # En producción, reemplazar por service discovery (Kubernetes, Consul, etc.)
```

### Cómo validar que las métricas existen

**1. Verificar que el endpoint responde:**
```bash
curl -s http://localhost:8080/actuator/prometheus | head -20
```
Debe retornar líneas en formato `# HELP` / `# TYPE` / `metric_name{...} value`.

**2. Verificar métricas JVM base (siempre presentes):**
```bash
curl -s http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes
curl -s http://localhost:8080/actuator/prometheus | grep process_uptime_seconds
```

**3. Verificar métricas de idempotencia (aparecen tras el primer request mutante):**
```bash
# Primero ejecutar al menos un request con Idempotency-Key, luego:
curl -s http://localhost:8080/actuator/prometheus | grep idempotency_
```
Las métricas de idempotencia usan lazy registration — no aparecen hasta que se ejecuta el primer path que las registra.

**4. Verificar links del actuator:**
```bash
curl -s http://localhost:8080/actuator | python3 -m json.tool | grep prometheus
```

**5. Validación programática (test de integración):**
El test `ApplicationPrometheusEndpointTest` valida automáticamente:
- Endpoint retorna HTTP 200
- Content-Type es `text/plain`
- Body contiene `jvm_memory_used_bytes` y `process_uptime_seconds`
- El endpoint aparece en los links del actuator

```bash
./gradlew :bootstrap:test --tests "co.edu.uniquindio.triage.ApplicationPrometheusEndpointTest"
```
