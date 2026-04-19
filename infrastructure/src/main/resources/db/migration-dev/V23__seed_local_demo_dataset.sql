-- Dataset demo SOLO para local/dev.
-- Objetivo: poblar un entorno útil y realista al primer arranque sobre DB vacía.

-- ---------------------------------------------------------------------------
-- 1) Usuarios demo (password compartida local: admin123)
-- ---------------------------------------------------------------------------
INSERT INTO users (username, email, identification, first_name, last_name, role, active, password_hash)
VALUES
    ('staff_registro', 'laura.gomez@uniquindio.edu.co', '90010001', 'Laura', 'Gómez', 'STAFF', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('staff_admisiones', 'camilo.rojas@uniquindio.edu.co', '90010002', 'Camilo', 'Rojas', 'STAFF', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('staff_financiero', 'natalia.mejia@uniquindio.edu.co', '90010003', 'Natalia', 'Mejía', 'STAFF', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('staff_bienestar', 'sofia.castano@uniquindio.edu.co', '90010004', 'Sofía', 'Castaño', 'STAFF', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('staff_homologa', 'daniel.lopez@uniquindio.edu.co', '90010005', 'Daniel', 'López', 'STAFF', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),

    ('ana_martinez', 'ana.martinez@uq.edu.co', '10010001', 'Ana', 'Martínez', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('juan_perez', 'juan.perez@uq.edu.co', '10010002', 'Juan', 'Pérez', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('laura_sanchez', 'laura.sanchez@uq.edu.co', '10010003', 'Laura', 'Sánchez', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('mateo_giraldo', 'mateo.giraldo@uq.edu.co', '10010004', 'Mateo', 'Giraldo', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('sara_morales', 'sara.morales@uq.edu.co', '10010005', 'Sara', 'Morales', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('david_arias', 'david.arias@uq.edu.co', '10010006', 'David', 'Arias', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('paula_torres', 'paula.torres@uq.edu.co', '10010007', 'Paula', 'Torres', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('andres_quintero', 'andres.quintero@uq.edu.co', '10010008', 'Andrés', 'Quintero', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('maria_ospina', 'maria.ospina@uq.edu.co', '10010009', 'María', 'Ospina', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('sebastian_villa', 'sebastian.villa@uq.edu.co', '10010010', 'Sebastián', 'Villa', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('valentina_diaz', 'valentina.diaz@uq.edu.co', '10010011', 'Valentina', 'Díaz', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'),
    ('nicolas_castro', 'nicolas.castro@uq.edu.co', '10010012', 'Nicolás', 'Castro', 'STUDENT', TRUE, '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe');

-- ---------------------------------------------------------------------------
-- 2) Catálogos demo realistas
-- ---------------------------------------------------------------------------
INSERT INTO request_types (name, description, active)
VALUES
    ('Cambio de grupo', 'Solicitud para cambio de grupo por cruce de horario o capacidad.', TRUE),
    ('Inscripción extemporánea', 'Solicitud para inscripción posterior al cierre ordinario.', TRUE),
    ('Revisión de nota final', 'Solicitud para revisar una calificación final con soporte del docente.', TRUE),
    ('Derechos de grado', 'Solicitud relacionada con paz y salvo y validación para grado.', TRUE),
    ('Práctica profesional', 'Solicitud de acompañamiento o ajuste para práctica profesional.', TRUE);

INSERT INTO origin_channels (name, active)
VALUES
    ('Chat institucional', TRUE),
    ('Derivación de coordinación', TRUE),
    ('Mesa de ayuda académica', TRUE);

-- ---------------------------------------------------------------------------
-- 3) Business rules demo coherentes con el contrato actual
-- ---------------------------------------------------------------------------
INSERT INTO business_rules (name, description, condition_type, condition_value, resulting_priority, active, request_type_id)
SELECT 'Cambio de grupo urgente',
       'Alta prioridad cuando el estudiante reporta cruce y el plazo académico es corto.',
       'REQUEST_TYPE_AND_DEADLINE', '5', 'HIGH', TRUE, rt.id
FROM request_types rt WHERE rt.name = 'Cambio de grupo';

INSERT INTO business_rules (name, description, condition_type, condition_value, resulting_priority, active, request_type_id)
SELECT 'Inscripción extemporánea inmediata',
       'Alta prioridad para inscripciones extemporáneas cercanas al cierre.',
       'REQUEST_TYPE_AND_DEADLINE', '3', 'HIGH', TRUE, rt.id
FROM request_types rt WHERE rt.name = 'Inscripción extemporánea';

INSERT INTO business_rules (name, description, condition_type, condition_value, resulting_priority, active, request_type_id)
SELECT 'Revisión de nota estándar',
       'Prioridad media para revisiones de nota con soportes completos.',
       'REQUEST_TYPE', CAST(rt.id AS CHAR), 'MEDIUM', TRUE, rt.id
FROM request_types rt WHERE rt.name = 'Revisión de nota final';

INSERT INTO business_rules (name, description, condition_type, condition_value, resulting_priority, active, request_type_id)
SELECT 'Derechos de grado próximos',
       'Alta prioridad para trámites de grado con fecha cercana.',
       'REQUEST_TYPE_AND_DEADLINE', '7', 'HIGH', TRUE, rt.id
FROM request_types rt WHERE rt.name = 'Derechos de grado';

-- ---------------------------------------------------------------------------
-- 4) Dataset voluminoso de solicitudes + historial coherente
-- ---------------------------------------------------------------------------
CREATE TEMPORARY TABLE demo_seed_students (
    seq INT PRIMARY KEY,
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO demo_seed_students (seq, username)
VALUES
    (1, 'ana_martinez'),
    (2, 'juan_perez'),
    (3, 'laura_sanchez'),
    (4, 'mateo_giraldo'),
    (5, 'sara_morales'),
    (6, 'david_arias'),
    (7, 'paula_torres'),
    (8, 'andres_quintero'),
    (9, 'maria_ospina'),
    (10, 'sebastian_villa'),
    (11, 'valentina_diaz'),
    (12, 'nicolas_castro');

CREATE TEMPORARY TABLE demo_seed_staff (
    seq INT PRIMARY KEY,
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO demo_seed_staff (seq, username)
VALUES
    (1, 'staff_registro'),
    (2, 'staff_admisiones'),
    (3, 'staff_financiero'),
    (4, 'staff_bienestar'),
    (5, 'staff_homologa');

CREATE TEMPORARY TABLE demo_seed_scenarios (
    seq INT PRIMARY KEY,
    request_type_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    origin_channel_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    description_base VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    default_priority VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    deadline_days INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO demo_seed_scenarios (seq, request_type_name, origin_channel_name, description_base, default_priority, deadline_days)
VALUES
    (1, 'Cambio de grupo', 'Sistema web', 'Necesito cambio de grupo por cruce con una materia del mismo semestre y disponibilidad limitada.', 'HIGH', 7),
    (2, 'Inscripción extemporánea', 'Correo electrónico', 'Solicito inscripción extemporánea porque la orden de matrícula se reflejó tarde y ya pasó el cierre.', 'HIGH', 5),
    (3, 'Revisión de nota final', 'Derivación de coordinación', 'Solicito revisión de nota final porque el corte final no refleja el trabajo entregado y tengo soportes.', 'MEDIUM', 10),
    (4, 'Derechos de grado', 'Mesa de ayuda académica', 'Requiero validar paz y salvo y derechos de grado para no perder la fecha de ceremonia.', 'HIGH', 14),
    (5, 'Homologación', 'Ventanilla', 'Solicito homologación de asignaturas cursadas en otra institución con certificados adjuntos.', 'MEDIUM', 20),
    (6, 'Práctica profesional', 'Chat institucional', 'Necesito ajuste del proceso de práctica por cambio de empresa receptora y fechas de inicio.', 'MEDIUM', 18),
    (7, 'Reintegro', 'Sistema web', 'Solicito reintegro académico después de suspensión temporal por motivos médicos ya superados.', 'HIGH', 12),
    (8, 'Certificado académico', 'Correo electrónico', 'Necesito certificado académico en inglés para proceso de movilidad internacional.', 'LOW', NULL),
    (9, 'Cancelación de asignatura', 'Teléfono', 'Solicito cancelar una asignatura por cruce laboral y afectación del rendimiento.', 'MEDIUM', 8),
    (10, 'Otro', 'Ventanilla', 'Presento solicitud especial de acompañamiento para ajuste de carga académica en semestre actual.', 'LOW', NULL);

CREATE TEMPORARY TABLE demo_seed_seq (n INT PRIMARY KEY);
INSERT INTO demo_seed_seq (n)
VALUES
    (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
    (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
    (21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
    (31),(32),(33),(34),(35),(36),(37),(38),(39),(40),
    (41),(42),(43),(44),(45),(46),(47),(48),(49),(50),
    (51),(52),(53),(54),(55),(56),(57),(58);

CREATE TEMPORARY TABLE demo_seed_plan AS
SELECT
    seq.n,
    stu.username AS student_username,
    stf.username AS staff_username,
    sc.request_type_name,
    sc.origin_channel_name,
    CONCAT(
        sc.description_base,
        CASE MOD(seq.n, 12)
            WHEN 0 THEN ' El trámite está asociado al grupo 1 de la jornada de la mañana.'
            WHEN 1 THEN ' El caso corresponde a una materia del componente básico del plan de estudios.'
            WHEN 2 THEN ' La situación impacta una inscripción prevista para el siguiente corte académico.'
            WHEN 3 THEN ' El estudiante reporta cruce con una actividad obligatoria del programa.'
            WHEN 4 THEN ' El caso fue revisado previamente con el director de programa.'
            WHEN 5 THEN ' Se adjuntarán soportes académicos emitidos por la facultad correspondiente.'
            WHEN 6 THEN ' La solicitud afecta una asignatura cursada en modalidad presencial.'
            WHEN 7 THEN ' El estudiante necesita respuesta antes del cierre financiero del semestre.'
            WHEN 8 THEN ' Se solicita validación para continuar con el proceso administrativo respectivo.'
            WHEN 9 THEN ' La situación está relacionada con una asignatura inscrita en segundo corte.'
            WHEN 10 THEN ' El trámite está vinculado con requisitos exigidos por la coordinación académica.'
            ELSE ' El estudiante busca evitar retrasos en su proceso académico y administrativo.'
        END
    ) AS description,
    CASE
        WHEN seq.n BETWEEN 1 AND 8 THEN 'REGISTERED'
        WHEN seq.n BETWEEN 9 AND 16 THEN 'CLASSIFIED'
        WHEN seq.n BETWEEN 17 AND 26 THEN 'IN_PROGRESS'
        WHEN seq.n BETWEEN 27 AND 32 THEN 'ATTENDED'
        WHEN seq.n BETWEEN 33 AND 46 THEN 'CLOSED'
        WHEN seq.n BETWEEN 47 AND 52 THEN 'CANCELLED'
        ELSE 'REJECTED'
    END AS final_status,
    CASE
        WHEN seq.n BETWEEN 17 AND 46 THEN sc.default_priority
        ELSE NULL
    END AS priority,
    CASE
        WHEN seq.n BETWEEN 17 AND 46 THEN
            CASE MOD(seq.n, 5)
                WHEN 0 THEN 'Se prioriza por cercanía a fechas institucionales y posible afectación del calendario académico.'
                WHEN 1 THEN 'Se prioriza por impacto directo en continuidad, matrícula o cierre administrativo.'
                WHEN 2 THEN 'Se prioriza para dar respuesta oportuna antes del siguiente corte académico.'
                WHEN 3 THEN 'Se prioriza porque el trámite requiere validación de varias dependencias en cadena.'
                ELSE 'Se prioriza por la urgencia reportada por el estudiante y la fecha límite asociada.'
            END
        ELSE NULL
    END AS priority_justification,
    CASE
        WHEN seq.n BETWEEN 27 AND 46 THEN
            CASE MOD(seq.n, 4)
                WHEN 0 THEN 'Se contactó al estudiante y se validó la información necesaria para continuar el trámite.'
                WHEN 1 THEN 'Se realizó atención y se dejó constancia de los soportes revisados en el caso.'
                WHEN 2 THEN 'Se dio respuesta inicial y se orientó al estudiante sobre los siguientes pasos.'
                ELSE 'Se atendió la solicitud y se coordinó seguimiento con la dependencia responsable.'
            END
        ELSE NULL
    END AS attendance_observation,
    CASE
        WHEN seq.n BETWEEN 33 AND 46 THEN
            CASE MOD(seq.n, 4)
                WHEN 0 THEN 'La solicitud se cerró luego de confirmar la solución con el estudiante y registrar el resultado.'
                WHEN 1 THEN 'El trámite se completó y se notificó al estudiante con la respuesta final.'
                WHEN 2 THEN 'Se finalizó la gestión después de validar soportes y dejar trazabilidad del proceso.'
                ELSE 'La dependencia responsable confirmó el cierre exitoso de la solicitud.'
            END
        ELSE NULL
    END AS closing_observation,
    CASE
        WHEN seq.n BETWEEN 47 AND 52 THEN
            CASE MOD(seq.n, 3)
                WHEN 0 THEN 'El estudiante indicó que ya resolvió la situación por otra vía y no requiere continuidad del trámite.'
                WHEN 1 THEN 'La solicitud fue cancelada porque el estudiante decidió no continuar con el proceso.'
                ELSE 'El estudiante informó que ya no necesita gestión adicional sobre este caso.'
            END
        ELSE NULL
    END AS cancellation_reason,
    CASE
        WHEN seq.n BETWEEN 53 AND 58 THEN
            CASE MOD(seq.n, 3)
                WHEN 0 THEN 'La solicitud no cumple los criterios documentales mínimos para continuar el trámite.'
                WHEN 1 THEN 'No fue posible aprobar la solicitud porque la información entregada es insuficiente.'
                ELSE 'La solicitud fue rechazada por no cumplir las condiciones requeridas por la dependencia responsable.'
            END
        ELSE NULL
    END AS rejection_reason,
    CASE
        WHEN MOD(seq.n, 4) = 0 THEN TRUE
        ELSE FALSE
    END AS ai_suggested,
    CASE
        WHEN MOD(seq.n, 3) = 0 AND seq.n BETWEEN 17 AND 46 THEN TRUE
        ELSE FALSE
    END AS has_internal_note,
    CASE
        WHEN sc.deadline_days IS NULL THEN NULL
        ELSE DATE_ADD(DATE(TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR), INTERVAL sc.deadline_days DAY)
    END AS deadline,
    TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR AS registration_at,
    CASE
        WHEN seq.n BETWEEN 1 AND 8 THEN TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR
        WHEN seq.n BETWEEN 9 AND 16 THEN TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR + INTERVAL 6 HOUR
        WHEN seq.n BETWEEN 17 AND 26 THEN TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR + INTERVAL 1 DAY + INTERVAL 3 HOUR
        WHEN seq.n BETWEEN 27 AND 32 THEN TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR + INTERVAL 4 DAY + INTERVAL 5 HOUR
        WHEN seq.n BETWEEN 33 AND 46 THEN TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR + INTERVAL 7 DAY + INTERVAL 4 HOUR
        WHEN seq.n BETWEEN 47 AND 52 THEN TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR + INTERVAL 2 DAY
        ELSE TIMESTAMP('2026-01-15 08:00:00') + INTERVAL (seq.n * 31) HOUR + INTERVAL 14 HOUR
    END AS updated_at
FROM demo_seed_seq seq
JOIN demo_seed_students stu ON stu.seq = ((seq.n - 1) MOD 12) + 1
JOIN demo_seed_staff stf ON stf.seq = ((seq.n - 1) MOD 5) + 1
JOIN demo_seed_scenarios sc ON sc.seq = ((seq.n - 1) MOD 10) + 1;

INSERT INTO academic_requests (
    description,
    priority,
    status,
    deadline,
    registration_date,
    priority_justification,
    rejection_reason,
    applicant_id,
    responsible_id,
    origin_channel_id,
    request_type_id,
    ai_suggested,
    closing_observation,
    cancellation_reason,
    attendance_observation,
    created_at,
    updated_at
)
SELECT
    p.description,
    p.priority,
    p.final_status,
    p.deadline,
    p.registration_at,
    p.priority_justification,
    p.rejection_reason,
    applicant.id,
    CASE WHEN p.final_status IN ('IN_PROGRESS', 'ATTENDED', 'CLOSED') THEN responsible.id ELSE NULL END,
    oc.id,
    rt.id,
    p.ai_suggested,
    p.closing_observation,
    p.cancellation_reason,
    p.attendance_observation,
    p.registration_at,
    p.updated_at
FROM demo_seed_plan p
JOIN users applicant ON applicant.username = p.student_username
JOIN users responsible ON responsible.username = p.staff_username
JOIN request_types rt ON rt.name = p.request_type_name
JOIN origin_channels oc ON oc.name = p.origin_channel_name
ORDER BY p.n;

CREATE TEMPORARY TABLE demo_seed_request_ids AS
SELECT ar.id,
       p.n,
       p.student_username,
       p.staff_username,
       p.final_status,
       p.has_internal_note,
       p.registration_at
FROM academic_requests ar
JOIN users applicant ON applicant.id = ar.applicant_id
JOIN demo_seed_plan p ON ar.description = p.description
                     AND applicant.username = p.student_username
                     AND ar.registration_date = p.registration_at;

-- REGISTERED (siempre)
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'REGISTERED',
       'Solicitud registrada por el estudiante a través del canal reportado.',
       r.registration_at,
       r.id,
       applicant.id,
       NULL
FROM demo_seed_request_ids r
JOIN users applicant ON applicant.username = r.student_username;

-- CLASSIFIED (todo excepto REGISTERED / REJECTED)
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'CLASSIFIED',
       'La solicitud fue clasificada por el equipo responsable según su naturaleza.',
       r.registration_at + INTERVAL 6 HOUR,
       r.id,
       staff.id,
       NULL
FROM demo_seed_request_ids r
JOIN users staff ON staff.username = r.staff_username
WHERE r.final_status IN ('CLASSIFIED', 'IN_PROGRESS', 'ATTENDED', 'CLOSED', 'CANCELLED');

-- PRIORITIZED
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'PRIORITIZED',
       'Se registró la prioridad teniendo en cuenta las condiciones del caso y los tiempos de atención.',
       r.registration_at + INTERVAL 18 HOUR,
       r.id,
       staff.id,
       NULL
FROM demo_seed_request_ids r
JOIN users staff ON staff.username = r.staff_username
WHERE r.final_status IN ('IN_PROGRESS', 'ATTENDED', 'CLOSED');

-- ASSIGNED
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'ASSIGNED',
       'La solicitud fue asignada a un responsable para su seguimiento.',
       r.registration_at + INTERVAL 20 HOUR,
       r.id,
       staff.id,
       staff.id
FROM demo_seed_request_ids r
JOIN users staff ON staff.username = r.staff_username
WHERE r.final_status IN ('IN_PROGRESS', 'ATTENDED', 'CLOSED');

-- INTERNAL_NOTE (solo algunos)
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'INTERNAL_NOTE',
       'Se deja una nota interna para facilitar el seguimiento administrativo del trámite.',
       r.registration_at + INTERVAL 1 DAY,
       r.id,
       staff.id,
       staff.id
FROM demo_seed_request_ids r
JOIN users staff ON staff.username = r.staff_username
WHERE r.final_status IN ('IN_PROGRESS', 'ATTENDED', 'CLOSED')
  AND r.has_internal_note = TRUE;

-- ATTENDED
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'ATTENDED',
       'Se realizó la atención inicial y se registró evidencia del seguimiento.',
       r.registration_at + INTERVAL 4 DAY,
       r.id,
       staff.id,
       staff.id
FROM demo_seed_request_ids r
JOIN users staff ON staff.username = r.staff_username
WHERE r.final_status IN ('ATTENDED', 'CLOSED');

-- CLOSED
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'CLOSED',
       'La solicitud se cerró después de completar la gestión correspondiente.',
       r.registration_at + INTERVAL 7 DAY,
       r.id,
       staff.id,
       staff.id
FROM demo_seed_request_ids r
JOIN users staff ON staff.username = r.staff_username
WHERE r.final_status = 'CLOSED';

-- CANCELLED
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'CANCELLED',
       'La solicitud fue cancelada por decisión del estudiante.',
       r.registration_at + INTERVAL 2 DAY,
       r.id,
       applicant.id,
       NULL
FROM demo_seed_request_ids r
JOIN users applicant ON applicant.username = r.student_username
WHERE r.final_status = 'CANCELLED';

-- REJECTED
INSERT INTO request_history (action, observations, timestamp, request_id, performed_by_id, responsible_id)
SELECT 'REJECTED',
       'La solicitud fue rechazada porque no cumplía con los requisitos para continuar.',
       r.registration_at + INTERVAL 14 HOUR,
       r.id,
       admin.id,
       NULL
FROM demo_seed_request_ids r
JOIN users admin ON admin.username = 'admin'
WHERE r.final_status = 'REJECTED';

DROP TEMPORARY TABLE demo_seed_request_ids;
DROP TEMPORARY TABLE demo_seed_plan;
DROP TEMPORARY TABLE demo_seed_seq;
DROP TEMPORARY TABLE demo_seed_scenarios;
DROP TEMPORARY TABLE demo_seed_staff;
DROP TEMPORARY TABLE demo_seed_students;
