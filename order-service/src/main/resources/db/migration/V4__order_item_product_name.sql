-- Store snapshot product name on each order item for seller/buyer visibility
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
