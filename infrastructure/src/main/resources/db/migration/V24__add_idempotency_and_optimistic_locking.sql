ALTER TABLE academic_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER updated_at;

ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER password_hash;

ALTER TABLE request_types
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER active;

ALTER TABLE origin_channels
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER active;

ALTER TABLE business_rules
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER request_type_id;

CREATE TABLE idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope VARCHAR(150) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_status_code INT,
    response_headers TEXT,
    response_body LONGTEXT,
    response_content_type VARCHAR(150),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    CONSTRAINT uk_idempotency_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT chk_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_idempotency_status ON idempotency_keys(status);
CREATE INDEX idx_idempotency_scope_status ON idempotency_keys(scope, status);
