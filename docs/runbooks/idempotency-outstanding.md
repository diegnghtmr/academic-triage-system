# Runbook: 409 Outstanding atascado

**Síntoma**: El cliente recibe un `409 Conflict` con body `"Ya existe una solicitud en procesamiento"` y el estado no avanza.

---

## 1. Diagnóstico inicial

### 1a. Verificar en base de datos

```sql
-- Ver los registros PROCESSING más viejos
SELECT id, scope, principal_scope, idempotency_key, status, created_at, updated_at, expires_at
FROM idempotency_keys
WHERE status = 'PROCESSING'
ORDER BY created_at ASC
LIMIT 50;
```

Un registro legítimamente en curso tendrá `updated_at` reciente (segundos o pocos minutos atrás).  
Un registro stuck tendrá `updated_at` viejo — más de 5–10 minutos para operaciones normales.

### 1b. Métricas a revisar

- `idempotency.outstanding.total` — si el counter sube continuamente para la misma key, el request original sigue fallando antes de completar.
- `idempotency.claim.latency` — si la latencia de claim aumenta, puede haber contención de lock.
- Logs de error del servicio en el momento del request original.

---

## 2. Distinguir: en curso legítimo vs stuck

| Indicador | En curso legítimo | Stuck |
|-----------|-------------------|-------|
| `updated_at` | hace pocos segundos/minutos | hace más de 10 min |
| Logs del pod | request activo visible | sin actividad relacionada |
| `completed_at` | null | null |
| DB connections activas | puede haber una activa | ninguna activa |

---

## 3. Qué hacer si quedó colgado

Un registro PROCESSING que no avanza ocurre cuando el procesador lanzó una excepción **pero `cleanupClaim` también falló** (muy raro), o el pod murió antes de que `cleanupClaim` se ejecutara.

### Opción A — Borrar el registro stuck (resolución manual)

```sql
-- Verificar antes de borrar
SELECT * FROM idempotency_keys WHERE id = <id>;

-- Borrar solo si status = PROCESSING y updated_at es viejo
DELETE FROM idempotency_keys WHERE id = <id> AND status = 'PROCESSING';
```

Después de borrar, el cliente puede reintentar con la misma `Idempotency-Key` y el request se procesará como nuevo.

### Opción B — Esperar expiración automática

Cada registro tiene `expires_at`. El cleanup job (`IdempotencyCleanupScheduler`) corre cada 10 minutos y borrará el registro expirado automáticamente. Verificar cuándo expira:

```sql
SELECT id, expires_at, TIMESTAMPDIFF(MINUTE, NOW(), expires_at) AS minutes_until_expiry
FROM idempotency_keys
WHERE status = 'PROCESSING' AND id = <id>;
```

---

## 4. Prevención

- El `TransactionalIdempotencyExecutor` llama `cleanupClaim()` en `REQUIRES_NEW` cuando el procesador falla.
- Si el pod muere abruptamente (OOM, kill), el registro queda stuck hasta que expire.
- Para reducir ventana de stuck, considerar reducir `app.idempotency.ttl.default-days` en entornos de baja tolerancia.

---

## 5. Escalación

Si hay más de 5 registros stuck simultáneamente, investigar si hubo un despliegue fallido, un timeout de DB, o un bug en el procesador que lanza excepción **y** borra el cleanup.
