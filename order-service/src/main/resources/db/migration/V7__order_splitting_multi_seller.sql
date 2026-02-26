-- Migration: V7__order_splitting_multi_seller.sql
-- Adds order splitting support for multi-seller orders

-- Add order type (MAIN or SUB)
ALTER TABLE orders ADD COLUMN order_type VARCHAR(10) NOT NULL DEFAULT 'MAIN';

-- Add parent order UUID for sub-orders (nullable)
ALTER TABLE orders ADD COLUMN parent_order_uuid VARCHAR(36);

-- Add order group ID to link related orders (main + all sub-orders)
ALTER TABLE orders ADD COLUMN order_group_id VARCHAR(36);

-- Indexes for query performance
CREATE INDEX idx_orders_order_type ON orders(order_type);
CREATE INDEX idx_orders_parent_order_uuid ON orders(parent_order_uuid);
CREATE INDEX idx_orders_order_group_id ON orders(order_group_id);

-- Foreign key constraint (parent order must exist)
ALTER TABLE orders ADD CONSTRAINT fk_orders_parent 
    FOREIGN KEY (parent_order_uuid) REFERENCES orders(uuid) ON DELETE CASCADE;

-- Comment documentation
COMMENT ON COLUMN orders.order_type IS 'MAIN (buyer placed) or SUB (seller split)';
COMMENT ON COLUMN orders.parent_order_uuid IS 'UUID of parent order if this is a sub-order';
COMMENT ON COLUMN orders.order_group_id IS 'Groups related orders together (main + sub-orders)';
