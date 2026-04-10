-- SOLO local/dev/test: reaplicar admin conocido para desarrollo (ver README).
-- No incluir `classpath:db/migration-dev` en despliegues que no sean locales.

INSERT INTO users (username, email, identification, first_name, last_name, role, active, password_hash)
VALUES (
    'admin',
    'admin@uniquindio.edu.co',
    '000000000',
    'Administrador',
    'Sistema',
    'ADMIN',
    TRUE,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
);
