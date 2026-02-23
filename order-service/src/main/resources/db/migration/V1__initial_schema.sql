-- order-service: initial schema

CREATE TABLE IF NOT EXISTS orders (
    id             BIGSERIAL        PRIMARY KEY,
    uuid           VARCHAR(36)      NOT NULL UNIQUE,
    buyer_uuid     VARCHAR(36)      NOT NULL,
    total_amount   DOUBLE PRECISION NOT NULL,
    status         VARCHAR(20),
    payment_status VARCHAR(20),
    is_deleted     BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_uuid       ON orders(uuid);
CREATE INDEX IF NOT EXISTS idx_orders_buyer_uuid ON orders(buyer_uuid);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);

CREATE TABLE IF NOT EXISTS order_item (
    id           BIGSERIAL        PRIMARY KEY,
    product_uuid VARCHAR(36),
    seller_uuid  VARCHAR(36),
    price        DOUBLE PRECISION,
    quantity     INTEGER,
    order_id     BIGINT           REFERENCES orders(id)
);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id     ON order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_uuid ON order_item(product_uuid);

CREATE TABLE IF NOT EXISTS processed_events (
    id           BIGSERIAL    PRIMARY KEY,
    event_id     VARCHAR(64)  NOT NULL UNIQUE,
    topic        VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_events_event_id ON processed_events(event_id);
