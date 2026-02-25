-- payment-service: payment split table for revenue sharing

CREATE TABLE IF NOT EXISTS payment_split (
    id                    BIGSERIAL        PRIMARY KEY,
    payment_uuid          VARCHAR(36)      NOT NULL,
    order_uuid            VARCHAR(36)      NOT NULL,
    seller_uuid           VARCHAR(36)      NOT NULL,
    product_uuid          VARCHAR(36)      NOT NULL,
    item_amount           DOUBLE PRECISION NOT NULL,
    platform_fee_percent  DOUBLE PRECISION NOT NULL,
    platform_fee          DOUBLE PRECISION NOT NULL,
    delivery_fee          DOUBLE PRECISION NOT NULL,
    seller_payout         DOUBLE PRECISION NOT NULL,
    status                VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ps_payment_uuid ON payment_split(payment_uuid);
CREATE INDEX IF NOT EXISTS idx_ps_order_uuid   ON payment_split(order_uuid);
CREATE INDEX IF NOT EXISTS idx_ps_seller_uuid  ON payment_split(seller_uuid);
CREATE INDEX IF NOT EXISTS idx_ps_status       ON payment_split(status);
