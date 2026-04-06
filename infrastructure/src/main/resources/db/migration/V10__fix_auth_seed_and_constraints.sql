-- V10: Repair auth seed drift and keep admin seed compatible with aligned schema

DELETE FROM users WHERE username = 'admin';

INSERT INTO users (username, email, identification, full_name, role, active, password_hash)
VALUES (
    'admin',
    'admin@uniquindio.edu.co',
    '000000000',
    'Administrador Sistema',
    'ADMIN',
    TRUE,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
);
