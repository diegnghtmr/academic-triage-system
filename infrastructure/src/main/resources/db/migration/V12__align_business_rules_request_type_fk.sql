-- V12: Align business rules optional request type relationship with current JPA metadata
-- Nota: ALTER TABLE en TiDB no soporta combinar ADD COLUMN y ADD CONSTRAINT (FK) en
-- la misma sentencia — el parser valida el FK antes de aplicar la nueva columna.
-- MariaDB/MySQL aceptan ambas formas; mantenemos sentencias separadas para portabilidad.

ALTER TABLE business_rules
    ADD COLUMN request_type_id BIGINT NULL;

ALTER TABLE business_rules
    ADD CONSTRAINT fk_business_rules_request_type
        FOREIGN KEY (request_type_id) REFERENCES request_types(id);
