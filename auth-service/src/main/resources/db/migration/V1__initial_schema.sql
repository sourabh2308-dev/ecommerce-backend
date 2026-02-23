-- auth-service: initial schema
-- Creates refresh_token table; user data lives in user-service's DB.

CREATE TABLE IF NOT EXISTS refresh_token (
    id           BIGSERIAL    PRIMARY KEY,
    token        VARCHAR(512) NOT NULL UNIQUE,
    user_uuid    VARCHAR(36)  NOT NULL,
    expiry_date  TIMESTAMP    NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_uuid ON refresh_token(user_uuid);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token     ON refresh_token(token);
