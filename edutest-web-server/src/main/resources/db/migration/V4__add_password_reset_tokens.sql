-- Add password_reset_tokens table for forgot-password / reset-password flow.
-- Single-use, time-limited tokens (default 30 min validity, configurable via
-- app.password-reset.token-validity-minutes).

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(80) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token
    ON password_reset_tokens (token);
