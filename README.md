# SourHub - Microservices E-Commerce Platform

SourHub is a Java 17, Spring Boot 3.2 microservices backend for a full e-commerce domain: authentication, users, catalog, orders, payments, and reviews. It is built as a distributed system with centralized config, service discovery, event-driven coordination, and observability.

## At A Glance

- Language/runtime: Java 17
- Frameworks: Spring Boot 3.2.x, Spring Cloud 2023.0.x
- Service topology: 9 Spring apps (gateway + 8 services)
- Data and messaging: PostgreSQL, Redis, Kafka (KRaft), Elasticsearch
- Edge/security: API Gateway with JWT validation and internal-secret propagation
- Reliability pattern: Order outbox + scheduled retry for order.created publication

## Documentation

### Core docs

| Document | Purpose |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture, infrastructure, communication patterns |
| [docs/API_OVERVIEW.md](docs/API_OVERVIEW.md) | Cross-service endpoint inventory |
| [docs/APPLICATION_FLOW.md](docs/APPLICATION_FLOW.md) | End-to-end business and saga flows |
| [docs/CODE_REFERENCE.md](docs/CODE_REFERENCE.md) | File and component reference |

### Service API docs

| Service | Documentation |
|---|---|
| API Gateway | [docs/services/API_GATEWAY.md](docs/services/API_GATEWAY.md) |
| Auth Service | [docs/services/AUTH_SERVICE.md](docs/services/AUTH_SERVICE.md) |
| User Service | [docs/services/USER_SERVICE.md](docs/services/USER_SERVICE.md) |
| Product Service | [docs/services/PRODUCT_SERVICE.md](docs/services/PRODUCT_SERVICE.md) |
| Order Service | [docs/services/ORDER_SERVICE.md](docs/services/ORDER_SERVICE.md) |
| Payment Service | [docs/services/PAYMENT_SERVICE.md](docs/services/PAYMENT_SERVICE.md) |
| Review Service | [docs/services/REVIEW_SERVICE.md](docs/services/REVIEW_SERVICE.md) |

## Architecture Summary

The API Gateway is the public entry point. It validates JWTs and forwards identity headers (`X-User-UUID`, `X-User-Role`, `X-User-Email`) plus `X-Internal-Secret` for service-to-service trust. Downstream services do not parse JWTs directly.

Order and payment are coordinated asynchronously via Kafka:

- `order.created` (Order -> Payment)
- `payment.completed` (Payment -> Order)

Order creation uses a durable outbox (`order_event_outbox`) so event publication is retried safely if Kafka is temporarily unavailable.

## Services

| Service | Role | Internal Port | Public Port |
|---|---|---:|---:|
| api-gateway | Entry point, routing, auth, distributed rate limiting (Redis) | 8080 | 8080 |
| auth-service | Login, refresh-token rotation, logout, password recovery orchestration | 8080 | - |
| user-service | Profiles, addresses, cart, wishlist, loyalty, support, seller onboarding | 8080 | - |
| product-service | Catalog, categories, variants, deals, search, inventory APIs | 8080 | - |
| order-service | Orders, status workflow, coupons, returns, tracking, invoices, outbox | 8080 | - |
| payment-service | Payment lifecycle, webhook verification, splits, dashboards | 8080 | - |
| review-service | Reviews, rating aggregation, voting, verified purchase checks | 8080 | - |
| eureka-server | Service discovery | 8761 | 8761 |
| config-server | Centralized config distribution | 8888 | 8888 |

## Infrastructure

| Component | Port | Notes |
|---|---:|---|
| PostgreSQL 15 | 5432 | 6 service-scoped databases initialized by [init.sql](init.sql) |
| Redis 7 | 6379 | Caching and gateway rate-limit counters |
| Elasticsearch 8 | 9200 | Product search index |
| Kafka (KRaft) | 29092 | Event streaming for sagas and integration events |
| Zipkin | 9411 | Trace visualization |
| Prometheus (full profile) | 9090 | Metrics scraping |
| Grafana (full profile) | 3000 | Dashboards |

## Repository Layout

```text
ecommerce-backend/
├── api-gateway/
├── auth-service/
├── user-service/
├── product-service/
├── order-service/
├── payment-service/
├── review-service/
├── eureka-server/
├── config-server/
├── docs/
├── grafana/
├── docker-compose.yml
├── init.sql
├── prometheus.yml
├── pom.xml
└── mvnw
```

## Getting Started

### Prerequisites

- Docker Engine + Docker Compose plugin
- Recommended: 8 GB+ RAM allocated to Docker

### 1. Clone

```bash
git clone https://github.com/sourabh2308-dev/ecommerce-backend.git
cd ecommerce-backend
```

### 2. Configure environment

You can use shell environment variables or an `.env` file in repo root.

Minimum required secrets for realistic local runs:

- `JWT_SECRET`
- `INTERNAL_SECRET`

Payment config for Razorpay mode:

- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`

Gateway mode selection:

- `PAYMENT_GATEWAY=razorpay` or `PAYMENT_GATEWAY=mock`

### 3. Start stack

Core stack:

```bash
docker compose up -d
```

Core + observability (`prometheus`, `grafana`):

```bash
docker compose --profile full up -d
```

### 4. Validate readiness

```bash
docker compose ps
curl -fsS http://localhost:8761/ >/dev/null && echo "Eureka up"
curl -fsS http://localhost:8080/actuator/health || true
```

## Quick API Smoke Flow

```bash
# Register buyer
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"John",
    "lastName":"Doe",
    "email":"john@example.com",
    "password":"password123",
    "phoneNumber":"9876543210",
    "role":"BUYER"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"password123"}'

# Use returned access token
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer <accessToken>"
```

## Build And Test

### Root build

```bash
./mvnw -q clean verify
```

### Run all tests

```bash
./mvnw -q test
```

### Run module tests

```bash
./mvnw -q -pl order-service test
./mvnw -q -pl payment-service test
```

### Build one service image in compose

```bash
docker compose up -d --build order-service
```

## Operations Cheat Sheet

```bash
# Tail service logs
docker compose logs -f api-gateway

# Restart one service
docker compose restart payment-service

# Stop all containers
docker compose down

# Full reset (containers + volumes)
docker compose down -v
```

## Security Model

- JWT issued by auth-service, verified at api-gateway.
- Gateway injects identity headers for downstream authorization.
- Internal endpoints are protected by shared `X-Internal-Secret`.
- Payment webhook endpoint is public but signature-verified.
- Distributed rate limiting is enforced at gateway using Redis counters.

## Event-Driven Flows

### Order -> Payment saga

1. Order is created and persisted.
2. Outbox row is created in same transaction.
3. After commit, outbox publisher emits `order.created`.
4. Payment service processes and emits `payment.completed`.
5. Order service updates payment/order status.

### Idempotency

- Payment service stores processed callback/event keys in `processed_events`.
- Duplicate gateway callbacks and duplicate events are safely ignored.

## Observability

- Tracing: Zipkin at `http://localhost:9411`
- Metrics: Prometheus at `http://localhost:9090` (full profile)
- Dashboards: Grafana at `http://localhost:3000` (full profile)
- Discovery: Eureka at `http://localhost:8761`

## Troubleshooting

| Symptom | What to check |
|---|---|
| Service not visible in Eureka | Verify service logs and config-server/eureka startup order |
| Gateway returns 401 unexpectedly | Confirm auth token validity and `JWT_SECRET` consistency |
| 403/unauthorized internal call | Confirm `INTERNAL_SECRET` consistency across services |
| Payment webhook rejected | Verify signature fields and gateway secret configuration |
| Kafka timeouts on startup | Allow broker warm-up, then restart dependent services |
| Elasticsearch not green | Yellow is expected for single-node development |

## Notes

- This repository currently focuses on backend services.
- The `full` compose profile enables monitoring services (Prometheus, Grafana).
- For deeper endpoint examples and field-level details, use the service docs under [docs/services](docs/services).
