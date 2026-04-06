ALTER TABLE academic_requests
    MODIFY COLUMN priority_justification VARCHAR(1000),
    MODIFY COLUMN rejection_reason VARCHAR(2000);
