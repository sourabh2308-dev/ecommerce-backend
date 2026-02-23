-- product-service: add image_url column
ALTER TABLE product ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);
