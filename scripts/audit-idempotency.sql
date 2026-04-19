-- ==============================================================================
-- Idempotency Operational Audit Script
-- Useful queries to understand the state of the idempotency layer.
-- ==============================================================================

-- 1. Count total keys by status
SELECT status, COUNT(*) AS total_keys
FROM idempotency_keys
GROUP BY status;

-- 2. Find keys that have expired but haven't been cleaned up
-- (If this number is growing, the IdempotencyCleanupScheduler might be failing)
SELECT COUNT(*) AS expired_uncleaned
FROM idempotency_keys
WHERE expires_at < NOW();

-- 3. Top 10 most replayed requests (based on last_seen_at - created_at)
-- This is a proxy to find which requests are being retried the most.
SELECT 
    scope,
    idempotency_key, 
    created_at, 
    last_seen_at, 
    TIMESTAMPDIFF(SECOND, created_at, last_seen_at) AS active_duration_seconds
FROM idempotency_keys
WHERE status = 'COMPLETED' AND last_seen_at > created_at
ORDER BY active_duration_seconds DESC
LIMIT 10;

-- 4. Find recently failed requests that are cached (Status 4xx / 5xx)
SELECT 
    scope, 
    idempotency_key, 
    response_status_code, 
    created_at
FROM idempotency_keys
WHERE response_status_code >= 400
ORDER BY created_at DESC
LIMIT 20;

-- 5. Outstanding/Stuck Keys
-- Keys that have been 'PROCESSING' for more than 5 minutes (possibly crashed threads)
SELECT 
    scope, 
    idempotency_key, 
    created_at, 
    expires_at
FROM idempotency_keys
WHERE status = 'PROCESSING'
  AND created_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE);

-- 6. AI Summary Cache Usage
SELECT 
    scope, 
    COUNT(*) AS ai_summaries_cached
FROM idempotency_keys
WHERE scope LIKE 'ai:%'
GROUP BY scope;
