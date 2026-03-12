-- V7: Seed initial data

-- Default admin user (password: admin123 - BCrypt encoded)
INSERT INTO users (username, email, identification, first_name, last_name, rol, active, password)
VALUES ('admin', 'admin@uniquindio.edu.co', '000000000', 'Administrador', 'Sistema', 'ADMIN', TRUE,
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- Default request types
INSERT INTO request_types (name, description, active) VALUES
('Certificado académico', 'Solicitud de certificados de notas, matrícula o constancias', TRUE),
('Cancelación de asignatura', 'Solicitud para cancelar una asignatura inscrita', TRUE),
('Homologación', 'Solicitud de homologación de asignaturas', TRUE),
('Reintegro', 'Solicitud de reintegro al programa académico', TRUE),
('Otro', 'Otras solicitudes académicas no categorizadas', TRUE);

-- Default origin channels
INSERT INTO origin_channels (name, active) VALUES
('Ventanilla', TRUE),
('Correo electrónico', TRUE),
('Sistema web', TRUE),
('Teléfono', TRUE);

-- Default business rules
INSERT INTO business_rules (name, description, condition_type, condition_value, resulting_priority, active) VALUES
('Plazo vencido', 'Solicitudes cuyo plazo ya expiró o vence hoy', 'DEADLINE_EXPIRED', 'true', 'HIGH', TRUE),
('Tipo reintegro', 'Las solicitudes de reintegro son prioridad alta', 'REQUEST_TYPE', 'Reintegro', 'HIGH', TRUE),
('Tipo cancelación', 'Las solicitudes de cancelación son prioridad media', 'REQUEST_TYPE', 'Cancelación de asignatura', 'MEDIUM', TRUE);
