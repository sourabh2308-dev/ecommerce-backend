-- product-service: initial schema

CREATE TABLE IF NOT EXISTS product (
    id              BIGSERIAL      PRIMARY KEY,
    uuid            VARCHAR(36)    NOT NULL UNIQUE,
    name            VARCHAR(255)   NOT NULL,
    description     VARCHAR(2000),
    price           DOUBLE PRECISION NOT NULL,
    stock           INTEGER        NOT NULL,
    category        VARCHAR(255)   NOT NULL,
    seller_uuid     VARCHAR(36)    NOT NULL,
    status          VARCHAR(20),
    is_deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    average_rating  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_reviews   INTEGER        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_uuid        ON product(uuid);
CREATE INDEX IF NOT EXISTS idx_product_seller_uuid ON product(seller_uuid);
CREATE INDEX IF NOT EXISTS idx_product_category    ON product(category);
CREATE INDEX IF NOT EXISTS idx_product_status      ON product(status);
