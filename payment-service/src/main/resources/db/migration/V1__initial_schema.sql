-- payment-service: initial schema

CREATE TABLE IF NOT EXISTS payment (
    id           BIGSERIAL        PRIMARY KEY,
    uuid         VARCHAR(36)      NOT NULL UNIQUE,
    order_uuid   VARCHAR(36)      NOT NULL,
    buyer_uuid   VARCHAR(36)      NOT NULL,
    amount       DOUBLE PRECISION,
    status       VARCHAR(20),
    created_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_uuid        ON payment(uuid);
CREATE INDEX IF NOT EXISTS idx_payment_order_uuid  ON payment(order_uuid);
CREATE INDEX IF NOT EXISTS idx_payment_buyer_uuid  ON payment(buyer_uuid);

CREATE TABLE IF NOT EXISTS processed_events (
    id           BIGSERIAL    PRIMARY KEY,
    event_id     VARCHAR(64)  NOT NULL UNIQUE,
    topic        VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pe_event_id ON processed_events(event_id);
