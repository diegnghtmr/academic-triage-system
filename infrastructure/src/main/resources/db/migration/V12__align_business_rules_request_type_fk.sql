-- V12: Align business rules optional request type relationship with current JPA metadata

ALTER TABLE business_rules
    ADD COLUMN request_type_id BIGINT NULL,
    ADD CONSTRAINT fk_business_rules_request_type
        FOREIGN KEY (request_type_id) REFERENCES request_types(id);
