-- V6: Coupons, shipment tracking, return requests, and tax/currency columns

-- ── Coupons ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupon (
    id                BIGSERIAL        PRIMARY KEY,
    uuid              VARCHAR(36)      NOT NULL UNIQUE,
    code              VARCHAR(50)      NOT NULL UNIQUE,
    discount_type     VARCHAR(20)      NOT NULL,
    discount_value    DOUBLE PRECISION NOT NULL,
    min_order_amount  DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_discount      DOUBLE PRECISION,
    total_usage_limit INTEGER          NOT NULL DEFAULT 100,
    per_user_limit    INTEGER          NOT NULL DEFAULT 1,
    current_usage     INTEGER          NOT NULL DEFAULT 0,
    valid_from        TIMESTAMP        NOT NULL,
    valid_until       TIMESTAMP        NOT NULL,
    is_active         BOOLEAN          NOT NULL DEFAULT TRUE,
    seller_uuid       VARCHAR(36),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupon(code);

-- Coupon usage tracking
CREATE TABLE IF NOT EXISTS coupon_usage (
    id          BIGSERIAL   PRIMARY KEY,
    coupon_id   BIGINT      NOT NULL REFERENCES coupon(id),
    buyer_uuid  VARCHAR(36) NOT NULL,
    order_uuid  VARCHAR(36) NOT NULL,
    used_at     TIMESTAMP   NOT NULL,
    UNIQUE(coupon_id, buyer_uuid, order_uuid)
);

CREATE INDEX IF NOT EXISTS idx_coupon_usage_buyer ON coupon_usage(buyer_uuid);

-- ── Shipment Tracking ──────────────────────────────────
CREATE TABLE IF NOT EXISTS shipment_tracking (
    id              BIGSERIAL    PRIMARY KEY,
    uuid            VARCHAR(36)  NOT NULL UNIQUE,
    order_uuid      VARCHAR(36)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    location        VARCHAR(255),
    description     VARCHAR(500),
    carrier         VARCHAR(100),
    tracking_number VARCHAR(100),
    event_time      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tracking_order ON shipment_tracking(order_uuid);

-- ── Return Requests ────────────────────────────────────
CREATE TABLE IF NOT EXISTS return_request (
    id            BIGSERIAL        PRIMARY KEY,
    uuid          VARCHAR(36)      NOT NULL UNIQUE,
    order_uuid    VARCHAR(36)      NOT NULL,
    buyer_uuid    VARCHAR(36)      NOT NULL,
    reason        VARCHAR(1000)    NOT NULL,
    status        VARCHAR(30)      NOT NULL DEFAULT 'PENDING',
    admin_notes   VARCHAR(1000),
    refund_amount DOUBLE PRECISION,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    resolved_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_return_order  ON return_request(order_uuid);
CREATE INDEX IF NOT EXISTS idx_return_buyer  ON return_request(buyer_uuid);
CREATE INDEX IF NOT EXISTS idx_return_status ON return_request(status);

-- ── Tax & Currency columns on orders ───────────────────
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_percent    DOUBLE PRECISION DEFAULT 18.0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_amount     DOUBLE PRECISION DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS currency       VARCHAR(10)      DEFAULT 'INR';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_code    VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount DOUBLE PRECISION DEFAULT 0;
