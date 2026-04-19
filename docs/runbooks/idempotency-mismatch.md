# Runbook: 422 Fingerprint mismatch

**Síntoma**: El cliente recibe `422 Unprocessable Entity` con body `"La clave ya fue usada con solicitud distinta"`.

---

## 1. Qué es un fingerprint mismatch

La idempotencia está garantizada por **scope + principal + key + fingerprint**.  
El fingerprint es un SHA-256 del scope, método HTTP, path, content-type, query params, y body (todos canonicalizados).

Un mismatch ocurre cuando se reutiliza la misma `Idempotency-Key` con un **request diferente** — cuerpo, headers o path distintos.

---

## 2. Causas típicas

| Causa | Descripción |
|-------|-------------|
| **Bug cliente** | El cliente genera la clave antes de construir el request y la reutiliza con datos modificados |
| **Reuso incorrecto de key** | El cliente reutiliza una key de un request anterior para una operación nueva (error de lógica) |
| **Serialización no determinista** | El cliente serializa JSON con keys en orden distinto en cada llamada (rompe el fingerprint canónico si hay campos a nivel raíz no normalizados) |
| **Intermediario que modifica body** | Un API gateway, proxy o middleware transforma el body antes de llegar al servicio |

---

## 3. Diagnóstico

### 3a. Consultar el registro almacenado

```sql
SELECT
  id,
  scope,
  principal_scope,
  idempotency_key,
  fingerprint,
  status,
  created_at,
  last_seen_at
FROM idempotency_keys
WHERE scope = '<scope>'
  AND principal_scope = '<user-id>'
  AND idempotency_key = '<key-del-cliente>';
```

Comparar el `fingerprint` almacenado con el del request que falló.  
El fingerprint del request entrante aparece en el log de debug de `CanonicalFingerprintService` si está habilitado.

### 3b. Habilitar log temporal (desarrollo/staging)

En `application-dev.yml`:
```yaml
logging.level.co.edu.uniquindio.triage.infrastructure.idempotency: DEBUG
```

Esto mostrará el JSON canónico antes de hashear.

---

## 4. Determinar si es bug cliente o reuso incorrecto

**Es bug cliente** si:
- El mismo endpoint se llama con la misma key pero el body tiene campos diferentes (datos dinámicos no controlados en el fingerprint)
- El cliente usa librerías que no garantizan orden de serialización JSON

**Es reuso incorrecto** si:
- El cliente reutiliza la key para una operación completamente diferente (ej: crear y luego asignar con la misma key)
- El cliente tiene un pool de keys fijo en lugar de generar una UUID por request idempotente

---

## 5. Resolución para el cliente

1. **Generar una nueva `Idempotency-Key`** (UUID v4) para el request modificado.
2. La key original permanece asociada al fingerprint original hasta que expire.
3. NO intentar reusar la misma key con datos diferentes — el sistema lo rechazará indefinidamente hasta que expire.

---

## 6. Métricas a monitorear

- `idempotency.mismatches.total` — spike indica un cliente con bug de generación de keys
- Un incremento gradual correlacionado con un despliegue sugiere cambio de serialización en el cliente

---

## 7. Si el mismatch lo causa un proxy/middleware

Verificar que ningún componente entre el cliente y el servicio modifique:
- El body del request
- El `Content-Type` header
- Los query parameters

El fingerprint se calcula **en el servidor** sobre lo que el servidor recibe.
