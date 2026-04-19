# Matriz de alineación del backend

## Fuentes analizadas

- Guía maestra del proyecto final.
- `AGENTS.md`.
- `docs/openapi-academic-triage.yaml`.
- `docs/diagram_state.png`.
- `docs/diagram_classes.jpeg`.
- `docs/ER.jpeg`.
- `docs/MER.jpeg`.
- Estado actual del código en `domain/`, `infrastructure/` y `bootstrap/`.

## Síntesis ejecutiva

El alcance funcional central está alineado. La guía maestra, el `AGENTS.md`, el OpenAPI y el código coinciden en el propósito del sistema: registrar, clasificar, priorizar, asignar, atender, cerrar y auditar solicitudes académicas con control por roles y soporte opcional de IA. Sin embargo, existen artefactos desactualizados que no conviene ignorar. El problema no es el núcleo del diseño, sino la consistencia documental.

La decisión de este repositorio debe ser simple: el contrato externo canónico es `docs/openapi-academic-triage.yaml`; el diseño arquitectónico canónico es `AGENTS.md`; el comportamiento canónico del dominio debe reflejarse en el código. Los diagramas deben actualizarse para acompañar esa realidad, no para competir con ella.

## Matriz de trazabilidad

| Tema | Guía maestra | AGENTS / OpenAPI / código | Estado | Decisión |
| --- | --- | --- | --- | --- |
| Propósito del sistema | Gestión integral de solicitudes académicas | Coincide en propósito y actores | Alineado | Mantener |
| RF-01 Registro de solicitudes | Obligatorio | Cubierto por `POST /requests` y por `AcademicRequest` | Alineado | Mantener |
| RF-02 Clasificación | Obligatorio | Cubierto por `PATCH /requests/{id}/classify` | Alineado | Mantener |
| RF-03 Priorización | Obligatorio | Cubierto por `PATCH /requests/{id}/prioritize` y `PriorityEngine` | Alineado | Mantener |
| RF-04 Ciclo de vida | Estados mínimos: registrada, clasificada, en atención, atendida, cerrada | Se extiende con `CANCELLED` y `REJECTED` | Alineado con extensión | Mantener extensión |
| RF-05 Asignación de responsables | Responsable activo y trazabilidad | Cubierto por `assign()` y validación de rol `STAFF` | Alineado | Mantener |
| RF-06 Historial auditable | Acción, fecha, usuario y observaciones | OpenAPI, dominio y JPA incluyen historial con responsable | Alineado | Mantener |
| RF-07 Consulta con filtros | Estado, tipo, prioridad, responsable | Cubierto por `GET /requests` con filtros paginados | Alineado | Mantener |
| RF-08 Cierre | Solo tras atención y con observación | Cubierto por `close()` y por estado terminal inmutable | Alineado | Mantener |
| RF-09 Resumen con IA | Opcional | OpenAPI incluye `GET /ai/summarize/{requestId}` | Alineado | Mantener |
| RF-10 Sugerencia con IA | Opcional | OpenAPI incluye `POST /ai/suggest-classification` | Alineado | Mantener |
| RF-11 Sistema sin IA | Obligatorio | `AGENTS.md` exige adaptador `NoOp` y operación sin IA | Alineado | Mantener |
| RF-12 API REST | Obligatorio | OpenAPI 3.0.3 ya define el contrato | Alineado | Mantener |
| RF-13 Autorización por roles | Obligatorio | Matriz de roles en `AGENTS.md` y restricciones por endpoint | Alineado con observaciones | Normalizar diferencias puntuales |
| Arquitectura | No prescribe hexagonal de forma explícita, pero exige diseño | `AGENTS.md` define hexagonal con DDD táctico | Alineado | Mantener como decisión de implementación |
| Persistencia | ORM y backend Spring Boot | JPA + Flyway + MariaDB | Alineado | Mantener |

## Desalineaciones detectadas y estado

### 1. Nombre del estado intermedio de atención

- La guía maestra y `diagram_state.png` usan la etiqueta funcional «En atención».
- `docs/diagram_classes.jpeg` usa `UNDER_CARE`.
- `AGENTS.md`, el OpenAPI y el código usan `IN_PROGRESS`.

Decisión de normalización:

- Nombre canónico en dominio, base de datos y API: `IN_PROGRESS`.
- Etiqueta funcional en español para frontend y documentos narrativos: «En atención».
- Acción pendiente: actualizar `docs/diagram_classes.jpeg` para reemplazar `UNDER_CARE` por `IN_PROGRESS` o por «En atención» si el diagrama se quiere mantener en español.

### 2. Estados terminales omitidos en el diagrama de clases

- La guía maestra admite al menos cinco estados mínimos.
- `AGENTS.md`, el OpenAPI y el código agregan `CANCELLED` y `REJECTED`.
- `docs/diagram_classes.jpeg` no los refleja.

Decisión de normalización:

- El modelo canónico del backend incluye siete estados: `REGISTERED`, `CLASSIFIED`, `IN_PROGRESS`, `ATTENDED`, `CLOSED`, `CANCELLED`, `REJECTED`.
- Acción pendiente: actualizar el diagrama de clases para reflejar los siete estados.

### 3. Endpoints de IA

Estado actual: resuelto.

- `AGENTS.md` fue actualizado para usar `POST /ai/suggest-classification` y `GET /ai/summarize/{requestId}`.
- El OpenAPI y la documentación interna ya usan los mismos endpoints.

Decisión aplicada:

- El contrato canónico sigue siendo el OpenAPI.
- `AGENTS.md` quedó ajustado a ese contrato.

### 4. Permisos para el resumen con IA

Estado actual: resuelto.

- `AGENTS.md` fue actualizado para que `GET /ai/summarize/{requestId}` quede habilitado solo para `STAFF` y `ADMIN`.
- El permiso del estudiante propietario ya no contradice el contrato OpenAPI.

Decisión aplicada:

- Se mantuvo la política documentada en el OpenAPI.
- La documentación de arquitectura y seguridad quedó alineada con ese alcance.

### 5. Campo de auditoría `performedBy` ausente en diagramas ER/MER

- La guía maestra exige usuario responsable en el historial.
- El OpenAPI expone `performedBy`.
- El dominio y `RequestHistoryJpaEntity` incluyen `performedById` / `performedBy`.
- `docs/ER.jpeg` y `docs/MER.jpeg` no muestran esa relación de forma consistente.

Decisión de normalización:

- `performedBy` es obligatorio en el historial del backend.
- Acción pendiente: agregar `performed_by_id` a los diagramas ER/MER.

### 6. Modelo relacional desactualizado frente al dominio actual

- `docs/MER.jpeg` no muestra de forma explícita campos ya presentes en el dominio y en JPA, por ejemplo `cancellation_reason`, `closing_observation`, `attendance_observation` y `ai_suggested`.

Decisión de normalización:

- El modelo relacional canónico debe reflejar todos los atributos persistidos en `AcademicRequestJpaEntity`.
- Acción pendiente: actualizar el MER.

### 7. Alcance de IA frente a la guía del curso

- La guía maestra exige implementar al menos una funcionalidad asistida por IA.
- `AGENTS.md` indica que la solución debe operar correctamente sin IA y que la integración permanece deshabilitada por defecto.

Decisión de normalización:

- No hay contradicción técnica. El backend debe soportar operación sin IA por defecto y, además, planificar al menos una capacidad real de IA como incremento optativo del proyecto final.
- Recomendación: priorizar `POST /ai/suggest-classification` antes que el resumen, porque impacta el flujo operativo más temprano.

### 8. Estructura de identidad del usuario

Estado actual: resuelto.

- El OpenAPI y el código inicial usaban `fullName`.
- Se detectó que para operaciones académicas (listados, reportes, personalización) es superior tener `firstName` y `lastName` por separado.
- Se realizó una refactorización integral en dominio, aplicación, infraestructura, base de datos y documentación.

Decisión aplicada:

- El sistema ahora maneja nombres y apellidos de forma independiente.
- El contrato OpenAPI se actualizó para reflejar este cambio.
- Se mantiene un método `getFullName()` en el dominio para conveniencia de visualización.

## Decisiones rectoras para el PRD y el backlog

1. El OpenAPI manda sobre los nombres y contratos HTTP.
2. `AGENTS.md` manda sobre la arquitectura hexagonal, módulos, políticas de Lombok, MapStruct y pruebas.
3. El código del dominio manda sobre el comportamiento transaccional y la máquina de estados.
4. Los diagramas se consideran artefactos explicativos. Si contradicen al contrato o al código, deben actualizarse.

## Implicaciones para la implementación

- El backend puede avanzar sin bloquearse por los diagramas, siempre que adopte como canon el OpenAPI y el modelo de dominio ya presente.
- Antes de cerrar el proyecto conviene actualizar `docs/diagram_classes.jpeg`, `docs/ER.jpeg` y `docs/MER.jpeg` para evitar ruido en sustentación, desarrollo y pruebas.
- El backlog del backend debe cubrir tanto historias funcionales como historias habilitadoras de arquitectura, seguridad, persistencia y pruebas.
