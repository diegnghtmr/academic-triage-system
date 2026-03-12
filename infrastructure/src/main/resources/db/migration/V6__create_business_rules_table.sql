-- V6: Create business rules table and junction table
CREATE TABLE business_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    condition_type VARCHAR(50) NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    resulting_priority VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_rules_priority CHECK (resulting_priority IN ('HIGH', 'MEDIUM', 'LOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Junction table: many-to-many between business_rules and academic_requests
CREATE TABLE request_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    CONSTRAINT fk_rr_rule FOREIGN KEY (rule_id) REFERENCES business_rules(id),
    CONSTRAINT fk_rr_request FOREIGN KEY (request_id) REFERENCES academic_requests(id),
    CONSTRAINT uq_request_rules UNIQUE (rule_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
