-- V2: Add soft-delete support to reviews
ALTER TABLE review ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_review_is_deleted ON review(is_deleted);
