-- V27: Seed de datos demo (lab/showcase) — usuarios, solicitudes, historial y reglas extra.
-- Contexto: Universidad del Quindío, perfil prod. Idempotente por versión Flyway:
-- corre solo una vez en cada DB. Para regenerar, DROP+CREATE la base y volver a arrancar.
--
-- Password hash compartida por todos los usuarios demo (bcrypt $2a$10):
--   raw  = 'Triage2026!'
--   hash = '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea'
-- Es solo para el lab; rotar antes de cualquier uso real.

-- =============================================================================
-- 1) USUARIOS DEMO  (1 admin + 2 staff + 6 estudiantes)
-- =============================================================================
INSERT INTO users (username, email, identification, first_name, last_name, role, active, password_hash, version)
VALUES
  ('admin.triage', 'admin.triage@uniquindio.edu.co',  '1094000001', 'Camila',  'Rojas',   'ADMIN',   TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('staff.lopez',  'andres.lopez@uniquindio.edu.co',  '1094000002', 'Andrés',  'López',   'STAFF',   TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('staff.perez',  'valeria.perez@uniquindio.edu.co', '1094000003', 'Valeria', 'Pérez',   'STAFF',   TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('est.maria',    'maria.rivera@uniquindio.edu.co',  '1094000010', 'María',   'Rivera',  'STUDENT', TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('est.juan',     'juan.castano@uniquindio.edu.co',  '1094000011', 'Juan',    'Castaño', 'STUDENT', TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('est.sofia',    'sofia.gomez@uniquindio.edu.co',   '1094000012', 'Sofía',   'Gómez',   'STUDENT', TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('est.diego',    'diego.henao@uniquindio.edu.co',   '1094000013', 'Diego',   'Henao',   'STUDENT', TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('est.laura',    'laura.mejia@uniquindio.edu.co',   '1094000014', 'Laura',   'Mejía',   'STUDENT', TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0),
  ('est.tomas',    'tomas.salazar@uniquindio.edu.co', '1094000015', 'Tomás',   'Salazar', 'STUDENT', TRUE, '$2a$10$ICA6Iewb9g1wET0CcjzfhuyFh1467gYauqWbeGWLsCPJw8HUKYvea', 0);

-- =============================================================================
-- 2) Variables locales con los IDs recién insertados y los del catálogo (V7).
-- =============================================================================
SET @u_maria   = (SELECT id FROM users WHERE username = 'est.maria');
SET @u_juan    = (SELECT id FROM users WHERE username = 'est.juan');
SET @u_sofia   = (SELECT id FROM users WHERE username = 'est.sofia');
SET @u_diego   = (SELECT id FROM users WHERE username = 'est.diego');
SET @u_laura   = (SELECT id FROM users WHERE username = 'est.laura');
SET @u_tomas   = (SELECT id FROM users WHERE username = 'est.tomas');
SET @s_lopez   = (SELECT id FROM users WHERE username = 'staff.lopez');
SET @s_perez   = (SELECT id FROM users WHERE username = 'staff.perez');
SET @ad_camila = (SELECT id FROM users WHERE username = 'admin.triage');

SET @t_cert = (SELECT id FROM request_types WHERE name = 'Certificado académico');
SET @t_canc = (SELECT id FROM request_types WHERE name = 'Cancelación de asignatura');
SET @t_hom  = (SELECT id FROM request_types WHERE name = 'Homologación');
SET @t_rein = (SELECT id FROM request_types WHERE name = 'Reintegro');
SET @t_otro = (SELECT id FROM request_types WHERE name = 'Otro');

SET @c_vent = (SELECT id FROM origin_channels WHERE name = 'Ventanilla');
SET @c_mail = (SELECT id FROM origin_channels WHERE name = 'Correo electrónico');
SET @c_web  = (SELECT id FROM origin_channels WHERE name = 'Sistema web');
SET @c_tel  = (SELECT id FROM origin_channels WHERE name = 'Teléfono');

-- =============================================================================
-- 3) SOLICITUDES — 15 con mix de estados/prioridades/canales/tipos.
--    Las descripciones son distintas para luego recuperar IDs por LIKE.
-- =============================================================================
INSERT INTO academic_requests
(description, priority, status, deadline, registration_date, priority_justification,
 applicant_id, responsible_id, origin_channel_id, request_type_id, ai_suggested,
 closing_observation, cancellation_reason, rejection_reason, attendance_observation,
 created_at, updated_at, version)
VALUES
-- 1. REGISTERED, sin prioridad ni responsable (pendiente de triage)
('Solicito certificado de notas de pregrado para presentar en convocatoria de prácticas profesionales.',
 NULL, 'REGISTERED', DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 NULL, @u_maria, NULL, @c_web, @t_cert, FALSE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 0),

-- 2. CLASSIFIED, prioridad MEDIUM, sin responsable aún
('Cancelación de la asignatura Cálculo III por incompatibilidad de horario con prácticas profesionales.',
 'MEDIUM', 'CLASSIFIED', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
 'Cancelación dentro del plazo académico, sin impacto sobre la matrícula vigente.',
 @u_juan, NULL, @c_vent, @t_canc, FALSE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 1),

-- 3. IN_PROGRESS, prioridad HIGH, asignada a staff.lopez
('Reintegro al programa de Ingeniería de Sistemas tras suspensión académica por bajo rendimiento en 2025-1.',
 'HIGH', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY),
 'Solicitud de reintegro requiere análisis del comité; la regla "Tipo reintegro" la marcó como alta.',
 @u_sofia, @s_lopez, @c_mail, @t_rein, TRUE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3),

-- 4. ATTENDED, prioridad MEDIUM, asignada a staff.perez
('Homologación de tres asignaturas cursadas en intercambio académico en Universidad de Salamanca.',
 'MEDIUM', 'ATTENDED', DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY),
 'Solicitud con documentación apostillada completa.',
 @u_diego, @s_perez, @c_mail, @t_hom, FALSE,
 NULL, NULL, NULL,
 'Se aprueban 2 de 3 asignaturas; la tercera requiere examen de validación adicional con el departamento.',
 DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 4),

-- 5. CLOSED, prioridad LOW, ciclo completo
('Certificado de matrícula del semestre 2026-1 para descuento del SISBEN ante la EPS.',
 'LOW', 'CLOSED', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY),
 NULL,
 @u_laura, @s_perez, @c_vent, @t_cert, FALSE,
 'Certificado entregado físicamente en ventanilla. Trámite cerrado sin novedades.',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 5),

-- 6. CANCELLED por el estudiante antes de clasificar
('Solicito retirar la asignatura Bases de Datos del semestre vigente.',
 NULL, 'CANCELLED', DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY),
 NULL,
 @u_tomas, NULL, @c_web, @t_canc, FALSE,
 NULL, 'El estudiante retiró voluntariamente la solicitud porque resolvió el conflicto de horario directamente con el docente.',
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 2),

-- 7. REJECTED, deadline pasado
('Reintegro inmediato al programa sin pasar por comité, por razones personales urgentes.',
 'HIGH', 'REJECTED', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY),
 NULL,
 @u_juan, @s_lopez, @c_mail, @t_rein, FALSE,
 NULL, NULL,
 'La normativa institucional no permite saltarse el comité; debe radicarse el reintegro formal por el portal con los soportes requeridos.',
 NULL, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY), 3),

-- 8. IN_PROGRESS prioridad LOW
('Solicito constancia de buena conducta para visa de intercambio académico en Argentina.',
 'LOW', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
 NULL,
 @u_maria, @s_perez, @c_web, @t_cert, FALSE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 2),

-- 9. CLASSIFIED con AI suggested
('Homologación de la asignatura Inglés Avanzado II con certificado TOEFL 95 puntos.',
 'LOW', 'CLASSIFIED', DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 'Documentación adjunta válida; trámite estándar sin urgencia.',
 @u_sofia, NULL, @c_mail, @t_hom, TRUE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 1),

-- 10. REGISTERED muy reciente, canal teléfono
('Estoy intentando matricular electivas pero el sistema rechaza la combinación de horarios. Necesito asesoría urgente.',
 NULL, 'REGISTERED', DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 6 HOUR),
 NULL,
 @u_diego, NULL, @c_tel, @t_otro, FALSE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, 0),

-- 11. IN_PROGRESS HIGH, deadline a 1 día
('Cancelación extemporánea de Microeconomía por incapacidad médica prolongada (certificado anexo de la EPS).',
 'HIGH', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY),
 'La regla "Plazo vencido" sugiere alta prioridad por proximidad de la fecha límite.',
 @u_laura, @s_lopez, @c_vent, @t_canc, TRUE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3),

-- 12. CLOSED hace mes
('Certificado de notas en inglés con apostilla de la Cancillería para postular a maestría en Canadá.',
 'MEDIUM', 'CLOSED', DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY),
 NULL,
 @u_tomas, @s_perez, @c_mail, @t_cert, FALSE,
 'Entregado por correo certificado al estudiante; apostilla emitida por la Cancillería. Trámite cerrado satisfactoriamente.',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), 5),

-- 13. CLASSIFIED, reintegro reciente
('Reintegro al programa de Licenciatura en Matemáticas después de aplazamiento de un semestre por motivos laborales.',
 'HIGH', 'CLASSIFIED', DATE_ADD(CURDATE(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
 'Solicitud de reintegro categorizada por la regla automática de tipo reintegro.',
 @u_maria, NULL, @c_web, @t_rein, TRUE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 1),

-- 14. ATTENDED prioridad baja
('Otra solicitud: cambio de jornada de diurna a nocturna para el próximo semestre por inicio de contrato laboral.',
 'LOW', 'ATTENDED', DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY),
 NULL,
 @u_juan, @s_perez, @c_vent, @t_otro, FALSE,
 NULL, NULL, NULL,
 'Cambio aprobado; el estudiante debe formalizar el ajuste de matrícula en el periodo establecido por registro académico.',
 DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 4),

-- 15. IN_PROGRESS cancelación con plazo cómodo
('Cancelación de Programación Web por enfermedad familiar comprobada con incapacidad de cuidador.',
 'MEDIUM', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 'Plazo aún vigente; canalizada por correo con documentación de soporte.',
 @u_sofia, @s_lopez, @c_mail, @t_canc, FALSE,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 2);

-- =============================================================================
-- 4) Recuperar IDs por descripción para enlazar historial.
-- =============================================================================
SET @r1  = (SELECT id FROM academic_requests WHERE description LIKE 'Solicito certificado de notas de pregrado%' LIMIT 1);
SET @r2  = (SELECT id FROM academic_requests WHERE description LIKE 'Cancelación de la asignatura Cálculo III%' LIMIT 1);
SET @r3  = (SELECT id FROM academic_requests WHERE description LIKE 'Reintegro al programa de Ingeniería de Sistemas%' LIMIT 1);
SET @r4  = (SELECT id FROM academic_requests WHERE description LIKE 'Homologación de tres asignaturas cursadas en intercambio%' LIMIT 1);
SET @r5  = (SELECT id FROM academic_requests WHERE description LIKE 'Certificado de matrícula del semestre 2026-1%' LIMIT 1);
SET @r6  = (SELECT id FROM academic_requests WHERE description LIKE 'Solicito retirar la asignatura Bases de Datos%' LIMIT 1);
SET @r7  = (SELECT id FROM academic_requests WHERE description LIKE 'Reintegro inmediato al programa sin pasar por comité%' LIMIT 1);
SET @r8  = (SELECT id FROM academic_requests WHERE description LIKE 'Solicito constancia de buena conducta%' LIMIT 1);
SET @r9  = (SELECT id FROM academic_requests WHERE description LIKE 'Homologación de la asignatura Inglés Avanzado II%' LIMIT 1);
SET @r10 = (SELECT id FROM academic_requests WHERE description LIKE 'Estoy intentando matricular electivas%' LIMIT 1);
SET @r11 = (SELECT id FROM academic_requests WHERE description LIKE 'Cancelación extemporánea de Microeconomía%' LIMIT 1);
SET @r12 = (SELECT id FROM academic_requests WHERE description LIKE 'Certificado de notas en inglés con apostilla%' LIMIT 1);
SET @r13 = (SELECT id FROM academic_requests WHERE description LIKE 'Reintegro al programa de Licenciatura en Matemáticas%' LIMIT 1);
SET @r14 = (SELECT id FROM academic_requests WHERE description LIKE 'Otra solicitud: cambio de jornada%' LIMIT 1);
SET @r15 = (SELECT id FROM academic_requests WHERE description LIKE 'Cancelación de Programación Web por enfermedad familiar%' LIMIT 1);

-- =============================================================================
-- 5) HISTORIAL — transiciones realistas por solicitud.
-- =============================================================================
INSERT INTO request_history (request_id, action, observations, timestamp, performed_by_id, responsible_id)
VALUES
-- R1 (REGISTERED)
(@r1, 'REGISTERED', 'Solicitud recibida por sistema web.', DATE_SUB(NOW(), INTERVAL 1 DAY), @u_maria, NULL),

-- R2 (CLASSIFIED)
(@r2, 'REGISTERED', 'Recibida en ventanilla; estudiante adjuntó horario de prácticas.', DATE_SUB(NOW(), INTERVAL 3 DAY), @u_juan, NULL),
(@r2, 'CLASSIFIED', 'Clasificada como cancelación estándar; prioridad MEDIA.', DATE_SUB(NOW(), INTERVAL 2 DAY), @s_perez, NULL),

-- R3 (IN_PROGRESS HIGH)
(@r3, 'REGISTERED', 'Reintegro recibido por correo electrónico.', DATE_SUB(NOW(), INTERVAL 7 DAY), @u_sofia, NULL),
(@r3, 'PRIORITIZED', 'Regla automática marcó como ALTA prioridad por tipo reintegro.', DATE_SUB(NOW(), INTERVAL 7 DAY), @s_perez, NULL),
(@r3, 'CLASSIFIED', 'Documentación validada; expediente listo para comité académico.', DATE_SUB(NOW(), INTERVAL 5 DAY), @s_perez, NULL),
(@r3, 'ASSIGNED', 'Asignada al staff Andrés López para gestionar el comité.', DATE_SUB(NOW(), INTERVAL 4 DAY), @ad_camila, @s_lopez),
(@r3, 'INTERNAL_NOTE', 'Solicitamos historia académica completa al programa.', DATE_SUB(NOW(), INTERVAL 2 DAY), @s_lopez, @s_lopez),

-- R4 (ATTENDED)
(@r4, 'REGISTERED', 'Homologación recibida por correo con documentos apostillados.', DATE_SUB(NOW(), INTERVAL 14 DAY), @u_diego, NULL),
(@r4, 'CLASSIFIED', 'Clasificada como homologación estándar.', DATE_SUB(NOW(), INTERVAL 13 DAY), @s_perez, NULL),
(@r4, 'ASSIGNED', 'Asignada a Valeria Pérez para revisión de equivalencias.', DATE_SUB(NOW(), INTERVAL 13 DAY), @ad_camila, @s_perez),
(@r4, 'ATTENDED', 'Se aprueban 2 de 3 asignaturas; la tercera requiere examen.', DATE_SUB(NOW(), INTERVAL 2 DAY), @s_perez, @s_perez),

-- R5 (CLOSED ciclo completo)
(@r5, 'REGISTERED', 'Solicitud de certificado de matrícula en ventanilla.', DATE_SUB(NOW(), INTERVAL 21 DAY), @u_laura, NULL),
(@r5, 'CLASSIFIED', 'Certificado estándar; prioridad baja.', DATE_SUB(NOW(), INTERVAL 20 DAY), @s_perez, NULL),
(@r5, 'ASSIGNED', 'Asignada a Valeria Pérez.', DATE_SUB(NOW(), INTERVAL 20 DAY), @ad_camila, @s_perez),
(@r5, 'ATTENDED', 'Certificado emitido y disponible para entrega.', DATE_SUB(NOW(), INTERVAL 14 DAY), @s_perez, @s_perez),
(@r5, 'CLOSED', 'Certificado entregado físicamente en ventanilla. Trámite cerrado.', DATE_SUB(NOW(), INTERVAL 9 DAY), @s_perez, @s_perez),

-- R6 (CANCELLED por el estudiante)
(@r6, 'REGISTERED', 'Solicitud de cancelación de Bases de Datos.', DATE_SUB(NOW(), INTERVAL 10 DAY), @u_tomas, NULL),
(@r6, 'CANCELLED', 'Estudiante retiró la solicitud porque resolvió conflicto con el docente.', DATE_SUB(NOW(), INTERVAL 8 DAY), @u_tomas, NULL),

-- R7 (REJECTED)
(@r7, 'REGISTERED', 'Reintegro express recibido por correo.', DATE_SUB(NOW(), INTERVAL 18 DAY), @u_juan, NULL),
(@r7, 'PRIORITIZED', 'Marcada alta por regla automática de tipo reintegro.', DATE_SUB(NOW(), INTERVAL 18 DAY), @s_perez, NULL),
(@r7, 'CLASSIFIED', 'Clasificada para revisión administrativa.', DATE_SUB(NOW(), INTERVAL 17 DAY), @s_perez, NULL),
(@r7, 'ASSIGNED', 'Asignada a Andrés López.', DATE_SUB(NOW(), INTERVAL 17 DAY), @ad_camila, @s_lopez),
(@r7, 'REJECTED', 'No procede sin paso por comité según normativa institucional.', DATE_SUB(NOW(), INTERVAL 16 DAY), @s_lopez, @s_lopez),

-- R8 (IN_PROGRESS)
(@r8, 'REGISTERED', 'Constancia de buena conducta solicitada por sistema web.', DATE_SUB(NOW(), INTERVAL 4 DAY), @u_maria, NULL),
(@r8, 'CLASSIFIED', 'Trámite estándar; prioridad baja.', DATE_SUB(NOW(), INTERVAL 3 DAY), @s_perez, NULL),
(@r8, 'ASSIGNED', 'Asignada a Valeria Pérez.', DATE_SUB(NOW(), INTERVAL 3 DAY), @ad_camila, @s_perez),

-- R9 (CLASSIFIED)
(@r9, 'REGISTERED', 'Homologación de inglés con TOEFL 95.', DATE_SUB(NOW(), INTERVAL 2 DAY), @u_sofia, NULL),
(@r9, 'CLASSIFIED', 'Documentación aceptada; pendiente asignación.', DATE_SUB(NOW(), INTERVAL 1 DAY), @s_perez, NULL),

-- R10 (REGISTERED muy reciente)
(@r10, 'REGISTERED', 'Reporte de incidencia por línea telefónica.', DATE_SUB(NOW(), INTERVAL 6 HOUR), @u_diego, NULL),

-- R11 (IN_PROGRESS HIGH urgente)
(@r11, 'REGISTERED', 'Cancelación extemporánea por incapacidad médica.', DATE_SUB(NOW(), INTERVAL 9 DAY), @u_laura, NULL),
(@r11, 'PRIORITIZED', 'Alta prioridad por proximidad de plazo.', DATE_SUB(NOW(), INTERVAL 9 DAY), @s_perez, NULL),
(@r11, 'CLASSIFIED', 'Documentación médica verificada con la EPS.', DATE_SUB(NOW(), INTERVAL 8 DAY), @s_perez, NULL),
(@r11, 'ASSIGNED', 'Asignada a Andrés López.', DATE_SUB(NOW(), INTERVAL 8 DAY), @ad_camila, @s_lopez),
(@r11, 'INTERNAL_NOTE', 'Pendiente confirmación del profesor titular de la asignatura.', DATE_SUB(NOW(), INTERVAL 1 DAY), @s_lopez, @s_lopez),

-- R12 (CLOSED hace mes)
(@r12, 'REGISTERED', 'Certificado con apostilla solicitado para Canadá.', DATE_SUB(NOW(), INTERVAL 30 DAY), @u_tomas, NULL),
(@r12, 'CLASSIFIED', 'Trámite con apostilla; coordinar con secretaría general.', DATE_SUB(NOW(), INTERVAL 28 DAY), @s_perez, NULL),
(@r12, 'ASSIGNED', 'Asignada a Valeria Pérez.', DATE_SUB(NOW(), INTERVAL 28 DAY), @ad_camila, @s_perez),
(@r12, 'ATTENDED', 'Apostilla obtenida en Cancillería; certificado emitido.', DATE_SUB(NOW(), INTERVAL 22 DAY), @s_perez, @s_perez),
(@r12, 'CLOSED', 'Entregado por correo certificado al estudiante.', DATE_SUB(NOW(), INTERVAL 18 DAY), @s_perez, @s_perez),

-- R13 (CLASSIFIED reintegro)
(@r13, 'REGISTERED', 'Reintegro a Licenciatura en Matemáticas.', DATE_SUB(NOW(), INTERVAL 5 DAY), @u_maria, NULL),
(@r13, 'PRIORITIZED', 'Marcada como ALTA por regla de tipo reintegro.', DATE_SUB(NOW(), INTERVAL 5 DAY), @s_perez, NULL),
(@r13, 'CLASSIFIED', 'Documentación inicial validada; espera asignación.', DATE_SUB(NOW(), INTERVAL 4 DAY), @s_perez, NULL),

-- R14 (ATTENDED)
(@r14, 'REGISTERED', 'Solicitud de cambio de jornada.', DATE_SUB(NOW(), INTERVAL 11 DAY), @u_juan, NULL),
(@r14, 'CLASSIFIED', 'Solicitud estándar; prioridad baja.', DATE_SUB(NOW(), INTERVAL 10 DAY), @s_perez, NULL),
(@r14, 'ASSIGNED', 'Asignada a Valeria Pérez.', DATE_SUB(NOW(), INTERVAL 10 DAY), @ad_camila, @s_perez),
(@r14, 'ATTENDED', 'Cambio aprobado pendiente de formalización por registro académico.', DATE_SUB(NOW(), INTERVAL 3 DAY), @s_perez, @s_perez),

-- R15 (IN_PROGRESS reciente)
(@r15, 'REGISTERED', 'Cancelación de Programación Web por enfermedad familiar.', DATE_SUB(NOW(), INTERVAL 2 DAY), @u_sofia, NULL),
(@r15, 'CLASSIFIED', 'Documentación recibida; queda en cola.', DATE_SUB(NOW(), INTERVAL 1 DAY), @s_perez, NULL),
(@r15, 'ASSIGNED', 'Asignada a Andrés López.', DATE_SUB(NOW(), INTERVAL 1 DAY), @ad_camila, @s_lopez);

-- =============================================================================
-- 6) REGLAS DE NEGOCIO EXTRA (las 3 del V7 ya existen).
-- =============================================================================
INSERT INTO business_rules (name, description, condition_type, condition_value, resulting_priority, active, version)
VALUES
('Plazo crítico (3 días)',
 'Solicitudes a 3 días o menos de su deadline son prioridad alta.',
 'DEADLINE', '3', 'HIGH', TRUE, 0),
('Homologación rutinaria',
 'Las homologaciones suelen tener documentación estándar; prioridad media.',
 'REQUEST_TYPE', '0', 'MEDIUM', TRUE, 0);

-- Vincular la regla "Homologación rutinaria" con el request_type real.
-- COLLATE explícito para evitar mezcla bajo TiDB (utf8mb4_0900_ai_ci vs utf8mb4_unicode_ci).
UPDATE business_rules br
SET br.request_type_id = (SELECT id FROM request_types WHERE name = 'Homologación'),
    br.condition_value = CAST((SELECT id FROM request_types WHERE name = 'Homologación') AS CHAR) COLLATE utf8mb4_unicode_ci
WHERE br.name = 'Homologación rutinaria';
