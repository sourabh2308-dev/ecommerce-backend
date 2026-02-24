-- user-service: seed default admin account
-- This runs once via Flyway and is idempotent due to ON CONFLICT(email).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (
    uuid,
    first_name,
    last_name,
    email,
    phone_number,
    password,
    role,
    status,
    email_verified,
    is_approved,
    is_deleted,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid()::text,
    'Admin',
    'User',
    'admin@admin.com',
    '9999999999',
    crypt('admin@123', gen_salt('bf', 10)),
    'ADMIN',
    'ACTIVE',
    TRUE,
    TRUE,
    FALSE,
    NOW(),
    NOW()
)
ON CONFLICT (email)
DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    phone_number = EXCLUDED.phone_number,
    password = EXCLUDED.password,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    email_verified = EXCLUDED.email_verified,
    is_approved = EXCLUDED.is_approved,
    is_deleted = EXCLUDED.is_deleted,
    updated_at = NOW();
