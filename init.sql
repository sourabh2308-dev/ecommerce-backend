-- =============================================================================
-- PostgreSQL Initialization Script
-- =============================================================================
-- This script is executed automatically by the PostgreSQL Docker container on
-- first startup (mounted into /docker-entrypoint-initdb.d/).  It creates the
-- individual databases required by each microservice in the e-commerce platform.
-- Each service connects to its own isolated database for data separation.
-- =============================================================================

-- Database for the User Service — stores user profiles and addresses
CREATE DATABASE user_db;

-- Database for the Auth Service — stores credentials, roles, and refresh tokens
CREATE DATABASE auth_db;

-- Database for the Order Service — stores orders, order items, and order status history
CREATE DATABASE order_db;

-- Database for the Review Service — stores product reviews and ratings
CREATE DATABASE review_db;

-- Database for the Product Service — stores product catalog, categories, and inventory
CREATE DATABASE product_db;

-- Database for the Payment Service — stores payment transactions and statuses
CREATE DATABASE payment_db;