-- Ampliar tabla idempotency_keys con principal_scope y campos de metadata/trazabilidad.
-- principal_scope NOT NULL DEFAULT '' garantiza que el índice único funcione correctamente
-- (NULL != NULL en MariaDB/MySQL haría que el constraint sea inútil).

ALTER TABLE idempotency_keys
    ADD COLUMN principal_scope VARCHAR(255) NOT NULL DEFAULT '' AFTER scope,
    ADD COLUMN resource_type   VARCHAR(100)                    AFTER response_content_type,
    ADD COLUMN resource_id     VARCHAR(255)                    AFTER resource_type,
    ADD COLUMN correlation_id  VARCHAR(255)                    AFTER resource_id,
    ADD COLUMN expires_at      DATETIME                        AFTER completed_at,
    ADD COLUMN last_seen_at    DATETIME                        AFTER expires_at;

-- Reemplazar constraint único anterior (scope, idempotency_key)
-- por la forma canónica de tres partes (scope, principal_scope, idempotency_key).
ALTER TABLE idempotency_keys
    DROP INDEX uk_idempotency_scope_key,
    ADD CONSTRAINT uk_idempotency_scope_principal_key
        UNIQUE (scope, principal_scope, idempotency_key);

-- Índice de soporte para barridos de TTL.
CREATE INDEX idx_idempotency_expires_at ON idempotency_keys (expires_at);
