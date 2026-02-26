-- Add product snapshot details for seller/buyer order visibility
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS product_category VARCHAR(100);

ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS product_image_url VARCHAR(1000);
