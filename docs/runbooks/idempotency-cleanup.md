# Runbook: Backlog de cleanup / expirados acumulados

**Síntoma**: La tabla `idempotency_keys` crece indefinidamente, el contador `idempotency.cleanup.deleted.total` no sube, o hay alertas de tamaño de tabla.

---

## 1. Detectar acumulación

### 1a. Tamaño de tabla y breakdown por estado

```sql
-- Cuántos registros hay y cuántos están expirados
SELECT
  status,
  COUNT(*) AS total,
  SUM(CASE WHEN expires_at IS NOT NULL AND expires_at < NOW() THEN 1 ELSE 0 END) AS expired,
  SUM(CASE WHEN expires_at IS NULL THEN 1 ELSE 0 END) AS no_expiry_set,
  MIN(created_at) AS oldest_record
FROM idempotency_keys
GROUP BY status;
```

### 1b. Métricas de cleanup

| Métrica | Qué indica |
|---------|------------|
| `idempotency.cleanup.runs.total` | Cuántas veces corrió el job |
| `idempotency.cleanup.deleted.total` | Filas eliminadas en total |
| `idempotency.cleanup.failures.total` | Cuántas veces falló |
| `idempotency.cleanup.duration` | Tiempo por ejecución |

Si `runs.total` no aumenta → el scheduler no está corriendo (ver sección 3).  
Si `runs.total` aumenta pero `deleted.total` no → no hay expirados (OK) o `expires_at` nunca se pobló.

---

## 2. Diagnóstico de registros sin `expires_at`

Si `no_expiry_set` > 0 en la query anterior, hay registros insertados antes de la migración al Batch 4A (que pobla `expires_at`). Estos NO serán borrados por el cleanup automático.

```sql
-- Ver cuántos registros legacy sin expires_at
SELECT COUNT(*), MIN(created_at), MAX(created_at)
FROM idempotency_keys
WHERE expires_at IS NULL;

-- Asignar expires_at retroactivamente (ajustar días según política)
UPDATE idempotency_keys
SET expires_at = created_at + INTERVAL 7 DAY
WHERE expires_at IS NULL AND status = 'COMPLETED';
```

---

## 3. Si el scheduler no está corriendo

Verificar que `@EnableScheduling` esté activo:
- Clase: `IdempotencyConfiguration` en el módulo infrastructure
- Clase: `IdempotencyCleanupScheduler` con `@Scheduled(cron = "${app.idempotency.cleanup.schedule:0 */10 * * * *}")`

Verificar la propiedad en el entorno:
```bash
# Debe mostrar el cron configurado
grep -i idempotency application.yml
```

Logs a buscar al iniciar la aplicación:
```
Registering bean 'idempotencyCleanupScheduler' for scheduled @Scheduled method
```

---

## 4. Si el cleanup falla repetidamente

La métrica `idempotency.cleanup.failures.total` sube → revisar logs:

```
ERROR - Idempotency cleanup batch failed
```

Causas comunes:
- DB no disponible o timeout → el scheduler reintenta en la siguiente ejecución, no hay pérdida
- Lock contention: el DELETE tarda demasiado → reducir `app.idempotency.cleanup.batch-size` (default: 500)
- Migración de tabla pendiente: verificar que `expires_at` tenga el índice `idx_idempotency_expires_at`

```sql
SHOW INDEX FROM idempotency_keys WHERE Key_name = 'idx_idempotency_expires_at';
```

---

## 5. Forzar cleanup manual de emergencia

Si la tabla está muy grande y el cleanup automático es insuficiente:

```sql
-- Borrar en lotes manuales para no bloquear
DELETE FROM idempotency_keys
WHERE id IN (
    SELECT id FROM (
        SELECT id FROM idempotency_keys
        WHERE expires_at IS NOT NULL AND expires_at < NOW()
        ORDER BY expires_at
        LIMIT 1000
    ) AS t
);
-- Repetir hasta que COUNT(*) sea razonable
```

---

## 6. Configuración del cleanup

Ajustar en `application.yml` o variables de entorno:

| Propiedad | Default | Descripción |
|-----------|---------|-------------|
| `app.idempotency.cleanup.schedule` | `0 */10 * * * *` | Cron de ejecución |
| `app.idempotency.cleanup.batch-size` | `500` | Filas por ejecución |
| `app.idempotency.ttl.default-days` | `7` | TTL para scopes de negocio |
| `app.idempotency.ttl.ai-days` | `1` | TTL para scopes `ai:*` |
