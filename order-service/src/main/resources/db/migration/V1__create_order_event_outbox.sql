CREATE TABLE IF NOT EXISTS order_event_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    order_uuid VARCHAR(64) NOT NULL UNIQUE,
    topic VARCHAR(128) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_event_outbox_published_created
    ON order_event_outbox (published, created_at);
