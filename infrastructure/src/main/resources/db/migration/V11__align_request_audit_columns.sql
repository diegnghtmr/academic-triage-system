-- V11: Align request audit columns with current JPA metadata for clean startup validation

ALTER TABLE academic_requests
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at DATETIME NULL;
