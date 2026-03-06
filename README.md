# SourHub — Microservices E-Commerce Platform

A production-grade e-commerce backend built with **Spring Boot 3.2**, **Spring Cloud 2023**, **Java 17**, and **Docker Compose**. The platform follows a microservices architecture with 6 domain services, an API gateway, service discovery, centralized configuration, and full observability.

---

## Documentation Index

| Document | What You'll Find |
|----------|-----------------|
| [Architecture Guide](docs/ARCHITECTURE.md) | System design, service topology, infrastructure, database schema, security model |
| [API Overview](docs/API_OVERVIEW.md) | Master endpoint table across all services with quick links |
| [Application Flow](docs/APPLICATION_FLOW.md) | End-to-end flows — auth, ordering, payment saga, returns, reviews |
| [Code Reference](docs/CODE_REFERENCE.md) | Complete file-by-file reference for every service |

### Per-Service API Documentation

> Each service doc includes: endpoints, request/response schemas, example JSON bodies, HTTP status codes, and **placeholders for Postman screenshots** — just drop your images into `docs/screenshots/` and update the image paths.

| Service | Doc | Description |
|---------|-----|-------------|
| API Gateway | [API_GATEWAY.md](docs/services/API_GATEWAY.md) | Routing, JWT validation, rate limiting, CORS |
| Auth Service | [AUTH_SERVICE.md](docs/services/AUTH_SERVICE.md) | Login, token refresh/rotation, logout, password recovery |
| User Service | [USER_SERVICE.md](docs/services/USER_SERVICE.md) | Registration, profile, addresses, cart, wishlist, notifications, loyalty, support |
| Product Service | [PRODUCT_SERVICE.md](docs/services/PRODUCT_SERVICE.md) | Catalog CRUD, categories, variants, images, flash deals, search |
| Order Service | [ORDER_SERVICE.md](docs/services/ORDER_SERVICE.md) | Orders, multi-seller split, coupons, returns, shipment tracking, invoices, dashboards |
| Payment Service | [PAYMENT_SERVICE.md](docs/services/PAYMENT_SERVICE.md) | Payment initiation, Razorpay integration, webhooks, seller payouts |
| Review Service | [REVIEW_SERVICE.md](docs/services/REVIEW_SERVICE.md) | Reviews, ratings, images, voting, verified purchases |

---

## System Overview

```
                         ┌──────────────────┐
                         │   Client / SPA   │
                         └────────┬─────────┘
                                  │
                         ┌────────▼─────────┐
                         │   API Gateway    │  :8080
                         │  (JWT + Routes)  │
                         └────────┬─────────┘
                                  │
          ┌──────────┬────────────┼────────────┬──────────┬──────────┐
          ▼          ▼            ▼            ▼          ▼          ▼
     ┌─────────┐┌─────────┐┌──────────┐┌──────────┐┌─────────┐┌─────────┐
     │  Auth   ││  User   ││ Product  ││  Order   ││ Payment ││ Review  │
     │ Service ││ Service ││ Service  ││ Service  ││ Service ││ Service │
     └────┬────┘└────┬────┘└────┬─────┘└────┬─────┘└────┬────┘└────┬────┘
          │          │          │            │           │          │
          └──────────┴──────────┴────┬───────┴───────────┴──────────┘
                                     │
              ┌──────────────────────┼──────────────────────┐
              ▼                      ▼                      ▼
        ┌───────────┐         ┌───────────┐          ┌───────────┐
        │ PostgreSQL│         │   Redis   │          │   Kafka   │
        │ (6 DBs)   │         │  (Cache)  │          │ (Events)  │
        └───────────┘         └───────────┘          └───────────┘
```

### Service Map & Ports

| Service | Internal Port | External Port | Database | Tech Stack |
|---------|:---:|:---:|----------|------------|
| **API Gateway** | 8080 | **8080** | — | Spring Cloud Gateway, JWT, Bucket4j |
| **Auth Service** | 8080 | — | auth_db | Spring Security, JWT (HS256) |
| **User Service** | 8080 | — | user_db | Spring Data JPA, Redis Cache |
| **Product Service** | 8080 | — | product_db | JPA, Redis, Elasticsearch, Kafka |
| **Order Service** | 8080 | — | order_db | JPA, Redis, Kafka, iText (PDF) |
| **Payment Service** | 8080 | — | payment_db | Razorpay SDK, Kafka |
| **Review Service** | 8080 | — | review_db | JPA, Kafka |
| **Eureka Server** | 8761 | 8761 | — | Netflix Eureka |
| **Config Server** | 8888 | 8888 | — | Spring Cloud Config |

### Infrastructure

| Component | Port | Purpose |
|-----------|:---:|---------|
| PostgreSQL 15 | 5432 | Primary data store (6 databases) |
| Redis 7 | 6379 | Caching, token blacklisting |
| Elasticsearch 8 | 9200 | Full-text product search |
| Apache Kafka (KRaft) | 29092 | Event streaming (order→payment→review) |
| Zipkin | 9411 | Distributed tracing |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Dashboards & alerting |

---

## Key Features

| Feature | Description |
|---------|-------------|
| **JWT Authentication** | Access tokens (15min) + refresh token rotation (7 days) |
| **Role-Based Access** | BUYER, SELLER, ADMIN — enforced at gateway + service level |
| **Multi-Seller Orders** | Orders auto-split into sub-orders per seller |
| **Payment Saga** | Order → Payment → Confirmation via Kafka events |
| **Razorpay Integration** | Production payment gateway with webhook verification |
| **Product Search** | Elasticsearch-powered full-text search with filters |
| **Returns & Exchanges** | Complete return workflow with admin approval |
| **Coupon System** | Percentage/flat discounts with usage limits |
| **Shipment Tracking** | Real-time tracking events per order |
| **PDF Invoices** | Auto-generated invoices, download + email delivery |
| **Loyalty Program** | Points earning, tier system, redemption |
| **Support Tickets** | Multi-message ticket system with admin assignment |
| **Flash Deals** | Time-limited promotions with auto-activation |
| **Product Variants** | Size/color/style with per-variant stock & pricing |
| **Category Hierarchy** | Nested categories with reordering |
| **Review System** | Star ratings, images, helpful voting, verified purchase badge |
| **Audit Logging** | Full change trail for orders (before/after state) |
| **Rate Limiting** | Per-IP token-bucket at gateway level |
| **Distributed Tracing** | Correlation ID propagation across all services |

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- 8 GB+ RAM (recommended for all containers)

### 1. Clone & Start

```bash
git clone https://github.com/sourabh2308-dev/ecommerce-backend.git
cd ecommerce-backend
docker compose up -d
```

### 2. Wait for Infrastructure

Services start in dependency order:

1. **Infrastructure** — PostgreSQL, Redis, Elasticsearch, Kafka, Zipkin
2. **Spring Cloud** — Eureka Server, Config Server
3. **Microservices** — Auth, User, Product, Order, Payment, Review
4. **Gateway** — API Gateway (available last)

Check readiness:

```bash
# All containers healthy
docker compose ps

# Eureka dashboard (should show all services registered)
open http://localhost:8761
```

### 3. Test the API

```bash
# Register a user
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "password123",
    "phoneNumber": "9876543210",
    "role": "BUYER"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "password": "password123"}'

# Use the accessToken from login response
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer <accessToken>"
```

### 4. Access Dashboards

| Dashboard | URL | Credentials |
|-----------|-----|-------------|
| Eureka | http://localhost:8761 | — |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Zipkin | http://localhost:9411 | — |

---

## Project Structure

```
ecommerce-backend/
├── api-gateway/          # Spring Cloud Gateway — JWT validation, routing, rate limiting
├── auth-service/         # Authentication — login, token refresh, logout, password recovery
├── user-service/         # User management — profiles, addresses, cart, wishlist, loyalty, support
├── product-service/      # Product catalog — CRUD, categories, variants, images, search, deals
├── order-service/        # Order processing — orders, coupons, returns, tracking, invoices
├── payment-service/      # Payment processing — Razorpay, webhooks, seller payouts
├── review-service/       # Reviews & ratings — CRUD, images, voting, verified purchases
├── eureka-server/        # Service discovery (Netflix Eureka)
├── config-server/        # Centralized configuration (Spring Cloud Config)
├── grafana/              # Grafana provisioning (dashboards, datasources)
├── docs/                 # Full documentation
│   ├── ARCHITECTURE.md
│   ├── API_OVERVIEW.md
│   ├── APPLICATION_FLOW.md
│   ├── CODE_REFERENCE.md
│   ├── services/         # Per-service API documentation
│   │   ├── API_GATEWAY.md
│   │   ├── AUTH_SERVICE.md
│   │   ├── USER_SERVICE.md
│   │   ├── PRODUCT_SERVICE.md
│   │   ├── ORDER_SERVICE.md
│   │   ├── PAYMENT_SERVICE.md
│   │   └── REVIEW_SERVICE.md
│   └── screenshots/      # Postman screenshots (add your own)
├── scripts/              # Helper scripts for Codespace environments
├── docker-compose.yml    # Full stack orchestration
├── init.sql              # Database initialization (creates 6 databases)
├── prometheus.yml        # Prometheus scrape config
└── pom.xml               # Root Maven POM
```

---

## Environment Variables

All services share these defaults (configurable via `docker-compose.yml`):

| Variable | Default | Used By |
|----------|---------|---------|
| `JWT_SECRET` | `mysupersecuresecretkeythatislongenough123` | Auth Service, API Gateway |
| `INTERNAL_SECRET` | `veryStrongInternalSecret123` | All services (service-to-service auth) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/{service}_db` | All services |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | All services |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | All services |
| `REDIS_HOST` | `redis` | User, Product, Order services |
| `REDIS_PORT` | `6379` | User, Product, Order services |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Product, Order, Payment, Review |
| `ELASTICSEARCH_URIS` | `http://elasticsearch:9200` | Product Service |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://eureka-server:8761/eureka/` | All services |
| `RAZORPAY_KEY_ID` | — | Payment Service |
| `RAZORPAY_KEY_SECRET` | — | Payment Service |
| `PAYMENT_GATEWAY` | `razorpay` | Payment Service |

> **Production**: Override `JWT_SECRET` and `INTERNAL_SECRET` with strong, unique values. Configure real Razorpay credentials.

---

## Useful Commands

```bash
# Start everything
docker compose up -d

# Start with monitoring stack
docker compose --profile full up -d

# View logs for a specific service
docker compose logs -f order-service

# Restart a single service after code change
docker compose up -d --build auth-service

# Stop everything
docker compose down

# Stop and remove volumes (full reset)
docker compose down -v

# Check which services are registered in Eureka
curl http://localhost:8761/eureka/apps | grep '<app>'
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Service not registered in Eureka | Wait 30-60s after startup; check `docker compose logs <service>` for errors |
| `Connection refused` to postgres | Ensure postgres container is healthy: `docker compose ps` |
| Kafka connection timeout | Kafka (KRaft) needs ~30s to initialize; restart dependent services |
| Elasticsearch yellow status | Normal for single-node dev setup; does not affect functionality |
| JWT token rejected | Ensure `JWT_SECRET` is identical in auth-service and api-gateway |
| `X-Internal-Secret` errors | Ensure `INTERNAL_SECRET` matches across all services |
| Out of memory | Increase Docker memory to 8GB+; reduce containers if needed |
