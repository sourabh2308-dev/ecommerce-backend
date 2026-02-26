-- Add return/exchange metadata for order return workflow
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_type   VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_reason VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_orders_return_type ON orders(return_type);
