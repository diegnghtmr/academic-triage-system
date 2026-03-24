-- V14: Add responsible audit reference to request history
ALTER TABLE request_history
    ADD COLUMN responsible_id BIGINT NULL AFTER performed_by_id,
    ADD CONSTRAINT fk_history_responsible FOREIGN KEY (responsible_id) REFERENCES users(id);

CREATE INDEX idx_history_responsible ON request_history(responsible_id);
