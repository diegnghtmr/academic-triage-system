-- V5: Create request history table
CREATE TABLE request_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    observations TEXT,
    timestamp DATETIME NOT NULL,
    request_id BIGINT NOT NULL,
    performed_by_id BIGINT NOT NULL,
    CONSTRAINT fk_history_request FOREIGN KEY (request_id) REFERENCES academic_requests(id),
    CONSTRAINT fk_history_performed_by FOREIGN KEY (performed_by_id) REFERENCES users(id),
    CONSTRAINT chk_history_action CHECK (action IN ('REGISTERED', 'CLASSIFIED', 'PRIORITIZED', 'ASSIGNED', 'ATTENDED', 'CLOSED', 'CANCELLED', 'REJECTED', 'INTERNAL_NOTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_history_request ON request_history(request_id);
CREATE INDEX idx_history_timestamp ON request_history(timestamp);
