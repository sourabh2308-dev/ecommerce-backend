-- V5: Cart, Wishlist, and Address tables for user-service

-- Shopping Cart Items
CREATE TABLE IF NOT EXISTS cart_items (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id),
    product_uuid   VARCHAR(36)  NOT NULL,
    product_name   VARCHAR(255),
    product_image  VARCHAR(500),
    price          DOUBLE PRECISION NOT NULL DEFAULT 0,
    quantity       INTEGER      NOT NULL DEFAULT 1,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    UNIQUE(user_id, product_uuid)
);

CREATE INDEX IF NOT EXISTS idx_cart_user_id ON cart_items(user_id);

-- Wishlist Items
CREATE TABLE IF NOT EXISTS wishlist_items (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id),
    product_uuid   VARCHAR(36)  NOT NULL,
    product_name   VARCHAR(255),
    product_image  VARCHAR(500),
    price          DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at     TIMESTAMP,
    UNIQUE(user_id, product_uuid)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_user_id ON wishlist_items(user_id);

-- User Addresses (multiple per user)
CREATE TABLE IF NOT EXISTS addresses (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id),
    uuid           VARCHAR(36)  NOT NULL UNIQUE,
    label          VARCHAR(50),
    full_name      VARCHAR(200) NOT NULL,
    phone          VARCHAR(15)  NOT NULL,
    address_line1  VARCHAR(255) NOT NULL,
    address_line2  VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    state          VARCHAR(100) NOT NULL,
    pincode        VARCHAR(10)  NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_address_user_id ON addresses(user_id);
