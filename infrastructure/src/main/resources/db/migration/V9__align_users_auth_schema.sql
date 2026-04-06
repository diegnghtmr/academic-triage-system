-- V9: Align users table with auth foundation contract

ALTER TABLE users
    ADD COLUMN full_name VARCHAR(150) NULL AFTER identification,
    ADD COLUMN password_hash VARCHAR(255) NULL AFTER active;

UPDATE users
SET full_name = TRIM(CONCAT(first_name, ' ', last_name)),
    password_hash = password;

ALTER TABLE users
    MODIFY COLUMN full_name VARCHAR(150) NOT NULL,
    MODIFY COLUMN identification VARCHAR(20) NOT NULL,
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;

ALTER TABLE users
    DROP COLUMN first_name,
    DROP COLUMN last_name,
    DROP COLUMN password;
