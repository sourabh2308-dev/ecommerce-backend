-- Migration: V5__categories.sql
-- Creates hierarchical product category structure

CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    parent_id BIGINT REFERENCES category(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT unique_category_name_per_parent UNIQUE (name, parent_id)
);

-- Indexes for query performance
CREATE INDEX idx_category_uuid ON category(uuid);
CREATE INDEX idx_category_parent_id ON category(parent_id);
CREATE INDEX idx_category_is_active ON category(is_active);
CREATE INDEX idx_category_parent_active ON category(parent_id, is_active, display_order);

-- Add category_id foreign key to product table
ALTER TABLE product ADD COLUMN category_id BIGINT REFERENCES category(id) ON DELETE SET NULL;
CREATE INDEX idx_product_category_id ON product(category_id);
