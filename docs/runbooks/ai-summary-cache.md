# Runbook: AI Summary Cache

**Contexto**: El sistema cachea los summaries generados por IA por versión de `AcademicRequest`. El cache evita llamadas duplicadas al proveedor de IA cuando el request no ha cambiado.

---

## 1. Cómo funciona el cache

- `GenerateSummaryService` verifica si existe un summary en `AiRequestSummaryJpaEntity` para el `requestId + version` actual.
- Si existe → devuelve el cacheado sin llamar al proveedor.
- Si no existe → llama al proveedor, guarda el resultado, lo devuelve.
- El `version` refleja la versión optimista JPA del `AcademicRequestJpaEntity`. Si el request cambia (nueva clasificación, prioridad, etc.), la versión sube y el cache del summary anterior queda obsoleto (se genera uno nuevo).

---

## 2. Detectar si se está evitando el costo duplicado

### 2a. Query de hits vs misses

```sql
-- Summaries generados por versión de request
SELECT
  request_id,
  COUNT(*) AS versiones_generadas,
  MAX(generated_at) AS ultima_generacion
FROM ai_request_summaries
GROUP BY request_id
ORDER BY versiones_generadas DESC
LIMIT 20;
```

Si un `request_id` tiene muchas versiones, el request ha cambiado muchas veces.  
Si solo tiene 1 versión y el resumen se consulta repetidamente → el cache está funcionando.

### 2b. Métricas de Spring AI / contador de llamadas al proveedor

Si está habilitado Actuator + Micrometer con el proveedor de IA configurado:
```
spring.ai.openai.*
```

Buscar en los logs el patrón:
```
DEBUG - AI provider called for requestId=<id>, version=<v>
DEBUG - Cache hit for requestId=<id>, version=<v>
```

---

## 3. Si se están regenerando summaries de más

### 3a. Verificar si el request cambia frecuentemente

```sql
-- Versiones de AcademicRequest con múltiples summaries
SELECT ar.id, ar.version AS current_version, COUNT(ars.id) AS cached_summaries
FROM academic_requests ar
LEFT JOIN ai_request_summaries ars ON ars.request_id = ar.id
GROUP BY ar.id, ar.version
HAVING cached_summaries > 1
ORDER BY cached_summaries DESC;
```

Si `current_version` es alto y `cached_summaries` coincide → el sistema generó un summary por cada versión (comportamiento esperado si el request cambió).

### 3b. Verificar si el proveedor de IA está habilitado

```bash
# En variables de entorno del pod
echo $APP_AI_PROVIDER   # debe ser "openai" o "none"
echo $SPRING_AI_OPENAI_API_KEY  # debe estar seteada si provider = openai
```

Si `APP_AI_PROVIDER=none`, el sistema usa `NoOpAiAssistantAdapter` y no genera summaries reales.

---

## 4. Si el cache nunca tiene hits (siempre regenera)

Causas posibles:
1. **Versiones incrementan muy frecuentemente**: cada actualización menor sube la versión y invalida el cache.
2. **Tabla `ai_request_summaries` se truncó o borró**: los summaries se perdieron.
3. **Bug en el lookup**: verificar que la query en `AiSummaryCachePersistenceAdapter` busca por `requestId + version` correctamente.

```sql
-- Verificar que la tabla tiene datos
SELECT COUNT(*), MIN(generated_at), MAX(generated_at) FROM ai_request_summaries;
```

---

## 5. Costo operativo

Cada llamada al proveedor tiene costo (tokens consumidos). Si el sistema llama más veces de lo esperado:
1. Verificar que `APP_AI_PROVIDER=none` en entornos que no requieren IA real.
2. Revisar si algún proceso actualiza AcademicRequests automáticamente y sube la versión innecesariamente.
3. El cache es por versión — si la versión no sube, el summary existente siempre se reutiliza.
