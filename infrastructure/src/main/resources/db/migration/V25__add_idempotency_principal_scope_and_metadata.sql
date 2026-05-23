-- Ampliar tabla idempotency_keys con principal_scope y campos de metadata/trazabilidad.
-- principal_scope NOT NULL DEFAULT '' garantiza que el índice único funcione correctamente
-- (NULL != NULL en MariaDB/MySQL haría que el constraint sea inútil).
--
-- Nota TiDB: cada ADD COLUMN debe ir en su propio ALTER TABLE porque TiDB valida
-- las referencias `AFTER xxx` contra el estado de la tabla al inicio de la sentencia,
-- y no permite forward references a columnas que se están agregando en el mismo ALTER.
-- En MariaDB/MySQL el comportamiento agrupado funciona; las sentencias separadas son
-- compatibles con ambos motores.

ALTER TABLE idempotency_keys
    ADD COLUMN principal_scope VARCHAR(255) NOT NULL DEFAULT '' AFTER scope;
ALTER TABLE idempotency_keys
    ADD COLUMN resource_type   VARCHAR(100)                    AFTER response_content_type;
ALTER TABLE idempotency_keys
    ADD COLUMN resource_id     VARCHAR(255)                    AFTER resource_type;
ALTER TABLE idempotency_keys
    ADD COLUMN correlation_id  VARCHAR(255)                    AFTER resource_id;
ALTER TABLE idempotency_keys
    ADD COLUMN expires_at      DATETIME                        AFTER completed_at;
ALTER TABLE idempotency_keys
    ADD COLUMN last_seen_at    DATETIME                        AFTER expires_at;

-- Reemplazar constraint único anterior (scope, idempotency_key)
-- por la forma canónica de tres partes (scope, principal_scope, idempotency_key).
-- Separado en dos sentencias por compatibilidad con TiDB.
ALTER TABLE idempotency_keys
    DROP INDEX uk_idempotency_scope_key;
ALTER TABLE idempotency_keys
    ADD CONSTRAINT uk_idempotency_scope_principal_key
        UNIQUE (scope, principal_scope, idempotency_key);

-- Índice de soporte para barridos de TTL.
CREATE INDEX idx_idempotency_expires_at ON idempotency_keys (expires_at);
