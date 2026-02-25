-- Widen image_url to TEXT so it can hold multiple semicolon-separated URLs
ALTER TABLE product ALTER COLUMN image_url TYPE TEXT;
