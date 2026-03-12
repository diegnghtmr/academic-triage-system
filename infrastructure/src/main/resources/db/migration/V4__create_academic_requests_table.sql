-- V4: Create academic requests table
CREATE TABLE academic_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description TEXT NOT NULL,
    priority VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    deadline DATE,
    registration_date DATETIME NOT NULL,
    priority_justification VARCHAR(500),
    rejection_observation VARCHAR(500),
    rejection_reason VARCHAR(500),
    applicant_id BIGINT NOT NULL,
    responsible_id BIGINT,
    origin_channel_id BIGINT NOT NULL,
    request_type_id BIGINT NOT NULL,
    ai_suggested BOOLEAN NOT NULL DEFAULT FALSE,
    closing_observation VARCHAR(500),
    CONSTRAINT fk_requests_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_requests_responsible FOREIGN KEY (responsible_id) REFERENCES users(id),
    CONSTRAINT fk_requests_origin_channel FOREIGN KEY (origin_channel_id) REFERENCES origin_channels(id),
    CONSTRAINT fk_requests_request_type FOREIGN KEY (request_type_id) REFERENCES request_types(id),
    CONSTRAINT chk_requests_status CHECK (status IN ('REGISTERED', 'CLASSIFIED', 'IN_PROGRESS', 'ATTENDED', 'CLOSED', 'CANCELLED', 'REJECTED')),
    CONSTRAINT chk_requests_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_requests_status ON academic_requests(status);
CREATE INDEX idx_requests_priority ON academic_requests(priority);
CREATE INDEX idx_requests_applicant ON academic_requests(applicant_id);
CREATE INDEX idx_requests_responsible ON academic_requests(responsible_id);
CREATE INDEX idx_requests_registration_date ON academic_requests(registration_date);
