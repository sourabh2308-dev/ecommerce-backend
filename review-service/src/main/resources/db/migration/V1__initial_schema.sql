-- review-service: initial schema

CREATE TABLE IF NOT EXISTS review (
    id           BIGSERIAL    PRIMARY KEY,
    uuid         VARCHAR(36)  UNIQUE,
    product_uuid VARCHAR(36),
    seller_uuid  VARCHAR(36),
    buyer_uuid   VARCHAR(36),
    rating       INTEGER,
    comment      VARCHAR(2000),
    created_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_review_product_uuid ON review(product_uuid);
CREATE INDEX IF NOT EXISTS idx_review_buyer_uuid   ON review(buyer_uuid);
