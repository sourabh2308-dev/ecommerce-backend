-- Product Images (multiple images per product)
CREATE TABLE IF NOT EXISTS product_image (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    image_url       TEXT         NOT NULL,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    alt_text        VARCHAR(255),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);
CREATE INDEX idx_product_image_product ON product_image(product_id);

-- Product Variants (size, color, etc.)
CREATE TABLE IF NOT EXISTS product_variant (
    id              BIGSERIAL PRIMARY KEY,
    uuid            VARCHAR(36)  NOT NULL UNIQUE,
    product_id      BIGINT       NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    variant_name    VARCHAR(50)  NOT NULL,
    variant_value   VARCHAR(100) NOT NULL,
    price_override  DOUBLE PRECISION,
    stock           INTEGER      NOT NULL DEFAULT 0,
    sku             VARCHAR(20)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    UNIQUE (product_id, variant_name, variant_value)
);
CREATE INDEX idx_product_variant_product ON product_variant(product_id);

-- Stock Movement audit log
CREATE TABLE IF NOT EXISTS stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    product_uuid    VARCHAR(36)  NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    stock_after     INTEGER      NOT NULL,
    reference       VARCHAR(500),
    created_at      TIMESTAMP    DEFAULT NOW()
);
CREATE INDEX idx_stock_movement_product ON stock_movement(product_uuid);

-- Flash Deals / Scheduled Sales
CREATE TABLE IF NOT EXISTS flash_deal (
    id               BIGSERIAL PRIMARY KEY,
    uuid             VARCHAR(36)  NOT NULL UNIQUE,
    product_id       BIGINT       NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    discount_percent DOUBLE PRECISION NOT NULL,
    start_time       TIMESTAMP    NOT NULL,
    end_time         TIMESTAMP    NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    seller_uuid      VARCHAR(36)  NOT NULL,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);
CREATE INDEX idx_flash_deal_active ON flash_deal(is_active, start_time, end_time);

-- Add low_stock_threshold to product table
ALTER TABLE product ADD COLUMN IF NOT EXISTS low_stock_threshold INTEGER DEFAULT 10;
