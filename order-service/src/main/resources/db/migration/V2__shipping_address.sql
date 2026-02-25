-- Add shipping address columns to orders table
ALTER TABLE orders ADD COLUMN shipping_name    VARCHAR(100);
ALTER TABLE orders ADD COLUMN shipping_address VARCHAR(255);
ALTER TABLE orders ADD COLUMN shipping_city    VARCHAR(100);
ALTER TABLE orders ADD COLUMN shipping_state   VARCHAR(100);
ALTER TABLE orders ADD COLUMN shipping_pincode VARCHAR(10);
ALTER TABLE orders ADD COLUMN shipping_phone   VARCHAR(15);
