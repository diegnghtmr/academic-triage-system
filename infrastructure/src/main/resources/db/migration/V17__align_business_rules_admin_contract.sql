-- V17: Align business rules schema with OpenAPI contract and normalize legacy data

-- 1. Extend name to VARCHAR(150)
ALTER TABLE business_rules MODIFY COLUMN name VARCHAR(150) NOT NULL;

-- 2. Change condition_value to TEXT for JSON support
ALTER TABLE business_rules MODIFY COLUMN condition_value TEXT NOT NULL;

-- 3. Normalize legacy seed data to match domain ConditionType
-- Map 'DEADLINE_EXPIRED' to 'DEADLINE'
UPDATE business_rules SET condition_type = 'DEADLINE' WHERE condition_type = 'DEADLINE_EXPIRED';

-- 4. Normalize condition_value to '0' or appropriate JSON-like string if needed for DEADLINE
-- In V7, 'Plazo vencido' had condition_value = 'true' for DEADLINE_EXPIRED.
-- Domain ConditionType.DEADLINE expects numeric (days). Let's use '0' (today/expired).
UPDATE business_rules SET condition_value = '0' WHERE name = 'Plazo vencido' AND condition_type = 'DEADLINE';

-- 5. Normalize REQUEST_TYPE condition_value to match ID or stay as name?
-- Current domain logic (BusinessRule.matches) might use name or ID.
-- However, OpenAPI describes it as ID on write. 
-- For now, we only normalize types to avoid validation errors in domain.
