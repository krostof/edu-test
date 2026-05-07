-- Add attempt_incidents table for cheating detection (soft warnings).
-- Recorded events from frontend listeners (alt-tab, large paste, devtools, etc.).
-- Visible to teacher when reviewing/grading the attempt.
--
-- Mirrors AttemptIncidentEntity. CREATE TABLE IF NOT EXISTS makes this safe on
-- dev DBs where Hibernate ddl-auto=update may have already created it.

CREATE TABLE IF NOT EXISTS attempt_incidents (
    id BIGSERIAL PRIMARY KEY,
    test_attempt_id BIGINT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    metadata VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_attempt_incidents_attempt
    ON attempt_incidents (test_attempt_id);

CREATE INDEX IF NOT EXISTS idx_attempt_incidents_attempt_type
    ON attempt_incidents (test_attempt_id, type);
