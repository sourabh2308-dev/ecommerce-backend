-- V4: Fix admin password to use Java-compatible BCrypt hash
-- The V2 migration used pgcrypto crypt() which produces a hash incompatible
-- with Spring's BCryptPasswordEncoder. This sets a proper $2a$ BCrypt hash.

UPDATE users
SET password = '$2a$10$3.HfDOkpgTVbqA0RrdapQ.IJCVVb4h7Dym6.xjPPoDQaXPq.WIQKa',
    updated_at = NOW()
WHERE email = 'admin@admin.com';
