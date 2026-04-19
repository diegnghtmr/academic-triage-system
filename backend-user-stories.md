# Historias de usuario del backend

## Criterios de organización

Estas historias cubren el backend del sistema y se derivan de la guía maestra, del contrato OpenAPI y de la arquitectura definida en `AGENTS.md`. Cada historia incluye su trazabilidad principal a los requisitos funcionales del curso. La prioridad usa tres niveles:

- Alta: imprescindible para un backend usable.
- Media: necesaria para operación completa o para administración.
- Baja: valiosa, pero no bloquea la operación base.

## Épica 1. Autenticación y autorización

### HU-01. Registro de usuario estudiante

Como visitante del sistema, necesito crear una cuenta con rol `STUDENT`, para registrar y consultar mis solicitudes académicas.

Prioridad: Alta  
Trazabilidad: RF-12, RF-13

Criterios de aceptación:

1. El endpoint `POST /auth/register` crea cuentas de estudiante sin requerir token.
2. El sistema valida unicidad de `username`, `email` e `identification`.
3. La solicitud requiere `firstName` y `lastName` por separado.
4. Si la solicitud no incluye token administrativo, el rol final siempre es `STUDENT`.
5. La respuesta retorna los datos básicos del usuario creado sin exponer la contraseña.

### HU-02. Registro administrativo con roles elevados

Como administrador, necesito registrar usuarios con rol `STAFF` o `ADMIN`, para delegar la operación del sistema.

Prioridad: Media  
Trazabilidad: RF-13

Criterios de aceptación:

1. Solo un usuario autenticado con rol `ADMIN` puede crear usuarios con roles distintos de `STUDENT`.
2. El sistema rechaza cualquier intento de un usuario no administrador de asignar roles elevados.
3. Los usuarios creados quedan activos por defecto, salvo que la política administrativa indique lo contrario.

### HU-03. Inicio de sesión con JWT

Como usuario registrado, necesito autenticarme y obtener un token JWT, para consumir la API según mi rol.

Prioridad: Alta  
Trazabilidad: RF-12, RF-13

Criterios de aceptación:

1. El endpoint `POST /auth/login` valida credenciales y retorna un JWT válido.
2. La respuesta incluye el tipo de token, su expiración y los datos básicos del usuario autenticado.
3. Si las credenciales son inválidas o la cuenta está inactiva, el sistema responde con error de autenticación.

### HU-04. Restricción de operaciones por rol

Como responsable de seguridad, necesito que cada endpoint respete permisos por rol, para evitar operaciones no autorizadas.

Prioridad: Alta  
Trazabilidad: RF-13

Criterios de aceptación:

1. Los endpoints públicos se limitan a autenticación y registro según contrato.
2. Las operaciones de clasificación, priorización, asignación, atención y cierre solo admiten usuarios autorizados.
3. Las operaciones administrativas de catálogos, reglas y usuarios se limitan a `ADMIN`.
4. Los errores de autorización se responden con códigos HTTP consistentes.

## Épica 2. Registro y consulta de solicitudes

### HU-05. Registro estructurado de solicitud académica

Como estudiante o funcionario autorizado, necesito registrar una solicitud académica con sus datos mínimos, para iniciar su atención formal.

Prioridad: Alta  
Trazabilidad: RF-01

Criterios de aceptación:

1. El endpoint `POST /requests` recibe tipo, canal de origen, descripción y fecha límite opcional.
2. La solicitud se crea con estado `REGISTERED`.
3. La fecha y hora de registro se generan en el backend.
4. El solicitante queda asociado a la solicitud.
5. El historial registra automáticamente la acción inicial de creación.

### HU-06. Consulta paginada de solicitudes

Como usuario autenticado, necesito consultar solicitudes con filtros, para hacer seguimiento operativo o personal.

Prioridad: Alta  
Trazabilidad: RF-07

Criterios de aceptación:

1. El endpoint `GET /requests` soporta filtros por estado, tipo, prioridad, responsable, solicitante y rango de fechas.
2. La respuesta es paginada e incluye metadatos de página.
3. Un estudiante solo ve sus propias solicitudes.
4. Un usuario `STAFF` o `ADMIN` puede ver todas las solicitudes, de acuerdo con su alcance operativo.

### HU-07. Consulta de detalle de una solicitud

Como usuario autorizado, necesito ver el detalle completo de una solicitud, para entender su estado actual y su trazabilidad.

Prioridad: Alta  
Trazabilidad: RF-06, RF-07

Criterios de aceptación:

1. El endpoint `GET /requests/{requestId}` retorna datos completos de la solicitud.
2. La respuesta incluye historial, prioridad, responsable asignado y observaciones terminales.
3. Un estudiante solo puede consultar solicitudes de su propiedad.
4. Si la solicitud no existe, la API responde con `404`.

## Épica 3. Gestión operativa del ciclo de vida

### HU-08. Clasificación de solicitudes

Como miembro del personal académico, necesito clasificar una solicitud registrada, para encaminar su tratamiento.

Prioridad: Alta  
Trazabilidad: RF-02, RF-04

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/classify` solo admite solicitudes en estado `REGISTERED`.
2. La operación asigna o corrige el tipo de solicitud.
3. El estado cambia a `CLASSIFIED`.
4. El historial registra la acción, el responsable y las observaciones opcionales.

### HU-09. Priorización con justificación

Como miembro del personal académico, necesito asignar una prioridad con justificación, para atender primero los casos críticos.

Prioridad: Alta  
Trazabilidad: RF-03, RF-04

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/prioritize` solo admite solicitudes en estado `CLASSIFIED`.
2. La prioridad solo puede ser `HIGH`, `MEDIUM` o `LOW`.
3. La justificación es obligatoria y queda persistida.
4. El historial registra la priorización.

### HU-10. Asignación de responsable activo

Como miembro del personal académico, necesito asignar una solicitud a un responsable activo, para que la atención tenga dueño claro.

Prioridad: Alta  
Trazabilidad: RF-05

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/assign` solo admite solicitudes `CLASSIFIED`.
2. El usuario asignado debe existir, estar activo y tener rol `STAFF`.
3. Al asignarse, la solicitud pasa a `IN_PROGRESS`.
4. El historial registra la asignación y el responsable.

### HU-11. Registro de atención de la solicitud

Como responsable asignado o personal autorizado, necesito registrar la atención realizada, para dejar evidencia de la resolución aplicada.

Prioridad: Alta  
Trazabilidad: RF-04, RF-06

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/attend` solo admite solicitudes en estado `IN_PROGRESS`.
2. La observación de atención es obligatoria.
3. La solicitud cambia a `ATTENDED`.
4. El historial registra la atención con fecha, usuario y observaciones.

### HU-12. Cierre formal de la solicitud

Como miembro del personal académico, necesito cerrar una solicitud atendida con observación obligatoria, para formalizar el fin del trámite.

Prioridad: Alta  
Trazabilidad: RF-08

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/close` solo admite solicitudes en estado `ATTENDED`.
2. La observación de cierre es obligatoria.
3. La solicitud cambia a `CLOSED`.
4. Una solicitud cerrada no admite más modificaciones de negocio.

### HU-13. Cancelación de solicitud

Como estudiante propietario o actor autorizado, necesito cancelar una solicitud con motivo explícito, para detener un trámite que ya no requiere gestión.

Prioridad: Alta  
Trazabilidad: RF-04

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/cancel` solo admite solicitudes `REGISTERED` o `CLASSIFIED`.
2. El motivo de cancelación es obligatorio.
3. La operación solo puede ser ejecutada por el propietario, `STAFF` o `ADMIN`.
4. El estado final queda en `CANCELLED`.

### HU-14. Rechazo administrativo de solicitud

Como administrador, necesito rechazar una solicitud registrada con motivo justificado, para detener casos que no cumplen política académica.

Prioridad: Media  
Trazabilidad: RF-04, RF-13

Criterios de aceptación:

1. El endpoint `PATCH /requests/{requestId}/reject` solo admite solicitudes `REGISTERED`.
2. El motivo de rechazo es obligatorio.
3. Solo `ADMIN` puede ejecutar esta acción.
4. La solicitud queda en `REJECTED` y no admite cambios posteriores.

## Épica 4. Historial y trazabilidad

### HU-15. Consulta del historial completo

Como usuario autorizado, necesito consultar el historial completo de una solicitud, para reconstruir todas las acciones ejecutadas sobre ella.

Prioridad: Alta  
Trazabilidad: RF-06

Criterios de aceptación:

1. El endpoint `GET /requests/{requestId}/history` retorna la secuencia completa de eventos.
2. Cada evento incluye acción, observaciones, fecha y responsable.
3. Un estudiante solo accede al historial de sus propias solicitudes.

### HU-16. Registro de notas internas

Como miembro del personal académico, necesito agregar notas internas sin cambiar el estado, para documentar contactos, verificaciones o seguimiento intermedio.

Prioridad: Media  
Trazabilidad: RF-06

Criterios de aceptación:

1. El endpoint `POST /requests/{requestId}/history` permite agregar notas de seguimiento.
2. La acción por defecto es `INTERNAL_NOTE`.
3. El registro se almacena en el historial sin alterar el estado actual de la solicitud.

## Épica 5. Catálogos y reglas de negocio

### HU-17. Administración de tipos de solicitud

Como administrador, necesito crear y actualizar tipos de solicitud, para clasificar los casos de manera consistente.

Prioridad: Media  
Trazabilidad: RF-02, RF-13

Criterios de aceptación:

1. Los endpoints de `request-types` permiten crear, consultar y actualizar tipos.
2. Los tipos tienen nombre, descripción y estado activo.
3. Solo `ADMIN` puede modificar el catálogo.

### HU-18. Administración de canales de origen

Como administrador, necesito gestionar los canales de origen, para representar de forma controlada de dónde provienen las solicitudes.

Prioridad: Media  
Trazabilidad: RF-01, RF-13

Criterios de aceptación:

1. Los endpoints de `origin-channels` permiten crear, consultar y actualizar canales.
2. Cada canal tiene nombre y estado activo.
3. Solo `ADMIN` puede modificar el catálogo.

### HU-19. Administración de reglas de priorización

Como administrador, necesito definir reglas de negocio para sugerir prioridad, para reducir arbitrariedad y mejorar tiempos de respuesta.

Prioridad: Media  
Trazabilidad: RF-03

Criterios de aceptación:

1. Los endpoints de `business-rules` permiten crear, listar, consultar, actualizar y desactivar reglas.
2. Cada regla define condición, valor de condición, prioridad resultante y estado activo.
3. Las reglas pueden asociarse a un tipo de solicitud cuando aplique.

## Épica 6. Administración de usuarios

### HU-20. Gestión administrativa de usuarios

Como administrador, necesito listar, consultar y actualizar usuarios, para controlar activación, datos y roles.

Prioridad: Media  
Trazabilidad: RF-13

Criterios de aceptación:

1. Los endpoints `/users`, `/users/{userId}` y `PUT /users/{userId}` funcionan según contrato.
2. El sistema permite activar o desactivar usuarios.
3. Solo `ADMIN` puede cambiar roles.
4. Un usuario puede consultar su propio perfil cuando la política lo permita.

## Épica 7. Reportes operativos

### HU-21. Métricas del tablero operativo

Como administrador, necesito consultar métricas agregadas del sistema, para monitorear carga, tiempos y desempeño operativo.

Prioridad: Media  
Trazabilidad: RF-07, RF-12

Criterios de aceptación:

1. El endpoint `GET /reports/dashboard` retorna totales por estado, tipo y prioridad.
2. La respuesta incluye tiempo promedio de resolución y responsables con más casos resueltos.
3. Solo `ADMIN` puede acceder al tablero.

## Épica 8. Asistencia opcional con IA

### HU-22. Sugerencia automática de clasificación y prioridad

Como miembro del personal académico, necesito recibir una sugerencia de tipo y prioridad a partir del texto de la solicitud, para acelerar la clasificación inicial.

Prioridad: Baja  
Trazabilidad: RF-10, RF-11

Criterios de aceptación:

1. El endpoint `POST /ai/suggest-classification` recibe la descripción de la solicitud.
2. La respuesta incluye tipo sugerido, prioridad sugerida, confianza y razonamiento.
3. La sugerencia no modifica la solicitud por sí misma.
4. Si el proveedor de IA no está disponible, el sistema responde de forma controlada sin romper el backend base.

### HU-23. Generación de resumen de caso

Como miembro del personal autorizado, necesito obtener un resumen textual del caso, para comprender rápidamente su estado e historial.

Prioridad: Baja  
Trazabilidad: RF-09, RF-11

Criterios de aceptación:

1. El endpoint `GET /ai/summarize/{requestId}` genera un resumen textual del estado e historial.
2. La operación no altera la solicitud ni su historial.
3. Si la IA no está disponible, el sistema responde con error controlado y el resto del backend sigue operativo.

### HU-24. Funcionamiento sin IA

Como arquitecto del sistema, necesito que todas las funcionalidades base operen sin dependencias de IA, para cumplir el requisito de resiliencia del proyecto.

Prioridad: Alta  
Trazabilidad: RF-11

Criterios de aceptación:

1. El registro, la consulta, la clasificación manual, la priorización, la asignación, la atención, el cierre y la seguridad funcionan sin clave de proveedor de IA.
2. La ausencia de configuración de IA no impide el arranque del backend.
3. La integración se desacopla mediante puerto y adaptador opcional.

## Épica 9. Historias habilitadoras de arquitectura

### HU-25. Implementación hexagonal del backend

Como equipo de desarrollo, necesitamos implementar el backend en módulos desacoplados, para mantener testabilidad y evolución controlada.

Prioridad: Alta  
Trazabilidad: soporte arquitectónico de RF-01 a RF-13

Criterios de aceptación:

1. El dominio no depende de Spring, JPA ni Lombok.
2. La capa de aplicación orquesta casos de uso mediante puertos.
3. La infraestructura implementa controladores, adaptadores, mapeadores, seguridad y persistencia.
4. Las pruebas de arquitectura verifican el cumplimiento de la separación por capas.

### HU-26. Persistencia versionada y trazable

Como equipo de desarrollo, necesitamos administrar el esquema con migraciones versionadas, para evitar deriva entre entorno, código y base de datos.

Prioridad: Alta  
Trazabilidad: soporte de RF-01 a RF-08

Criterios de aceptación:

1. El esquema se define mediante Flyway.
2. Las tablas, índices y claves foráneas cubren solicitudes, historial, usuarios, catálogos y reglas.
3. La base de datos no se actualiza mediante `ddl-auto: update`.

### HU-27. Cobertura de pruebas por capa

Como equipo de desarrollo, necesitamos pruebas por capa y por riesgo, para detectar regresiones funcionales y arquitectónicas.

Prioridad: Alta  
Trazabilidad: soporte transversal

Criterios de aceptación:

1. Existen pruebas unitarias de dominio para la máquina de estados y reglas.
2. Existen pruebas de casos de uso con dobles de prueba.
3. Existen pruebas de controladores, persistencia y arquitectura.
4. La operación crítica del backend tiene cobertura automatizada suficiente para sostener cambios.
