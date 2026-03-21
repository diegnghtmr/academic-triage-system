-- V8: Add missing columns to academic_requests and clean up orphaned column
-- cancellation_reason and attendance_observation were added to the domain model
-- rejection_observation was never mapped to any entity field (entity uses rejection_reason)

ALTER TABLE academic_requests ADD COLUMN cancellation_reason VARCHAR(2000);
ALTER TABLE academic_requests ADD COLUMN attendance_observation VARCHAR(2000);
ALTER TABLE academic_requests DROP COLUMN rejection_observation;
