-- user-service: initial schema
-- V1 - creates users + otp_verifications tables
-- NOTE: this reflects the JPA entity structure; ddl-auto=validate hereafter.

CREATE TABLE IF NOT EXISTS users (
    id               BIGSERIAL PRIMARY KEY,
    uuid             VARCHAR(36)  NOT NULL UNIQUE,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone_number     VARCHAR(15),
    password         VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_approved      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at    TIMESTAMP,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_users_email      ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_uuid       ON users(uuid);
CREATE INDEX IF NOT EXISTS idx_users_role       ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status     ON users(status);

CREATE TABLE IF NOT EXISTS otp_verifications (
    id             BIGSERIAL    PRIMARY KEY,
    otp_code       VARCHAR(255) NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    expiry_time    TIMESTAMP    NOT NULL,
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    attempt_count  INTEGER      NOT NULL DEFAULT 0,
    last_sent_at   TIMESTAMP,
    user_id        BIGINT       NOT NULL REFERENCES users(id),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_otp_user_id ON otp_verifications(user_id);
