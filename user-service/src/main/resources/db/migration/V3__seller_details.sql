-- user-service: seller verification details table

CREATE TABLE IF NOT EXISTS seller_details (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL UNIQUE REFERENCES users(id),
    business_name       VARCHAR(200) NOT NULL,
    business_type       VARCHAR(50)  NOT NULL,
    gst_number          VARCHAR(20),
    pan_number          VARCHAR(15),
    address_line1       VARCHAR(255) NOT NULL,
    address_line2       VARCHAR(255),
    city                VARCHAR(100) NOT NULL,
    state               VARCHAR(100) NOT NULL,
    pincode             VARCHAR(10)  NOT NULL,
    id_type             VARCHAR(30)  NOT NULL,
    id_number           VARCHAR(50)  NOT NULL,
    bank_account_number VARCHAR(30)  NOT NULL,
    bank_ifsc_code      VARCHAR(20)  NOT NULL,
    bank_name           VARCHAR(100) NOT NULL,
    submitted_at        TIMESTAMP,
    verified_at         TIMESTAMP,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sd_user_id ON seller_details(user_id);
