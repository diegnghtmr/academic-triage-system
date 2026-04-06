-- V13: Split full_name into first_name and last_name in users table

ALTER TABLE users ADD COLUMN first_name VARCHAR(75) AFTER identification;
ALTER TABLE users ADD COLUMN last_name VARCHAR(75) AFTER first_name;

-- Simple migration logic: first word to first_name, the rest to last_name
UPDATE users 
SET first_name = SUBSTRING_INDEX(full_name, ' ', 1),
    last_name = CASE 
        WHEN LOCATE(' ', full_name) > 0 THEN SUBSTRING(full_name, LOCATE(' ', full_name) + 1)
        ELSE ''
    END;

ALTER TABLE users MODIFY COLUMN first_name VARCHAR(75) NOT NULL;
ALTER TABLE users MODIFY COLUMN last_name VARCHAR(75) NOT NULL;

ALTER TABLE users DROP COLUMN full_name;
