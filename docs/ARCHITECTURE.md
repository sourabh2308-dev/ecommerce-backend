# Architecture Guide

This document describes the system architecture of the SourHub e-commerce platform — service responsibilities, infrastructure, communication patterns, security, and database design.

---

## Table of Contents

- [High-Level Architecture](#high-level-architecture)
- [Service Responsibilities](#service-responsibilities)
- [Infrastructure Components](#infrastructure-components)
- [Communication Patterns](#communication-patterns)
- [Security Architecture](#security-architecture)
- [Database Design](#database-design)
- [Caching Strategy](#caching-strategy)
- [Event-Driven Architecture (Kafka)](#event-driven-architecture-kafka)
- [Observability](#observability)
- [Deployment Architecture (Docker Compose)](#deployment-architecture-docker-compose)

---

## High-Level Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                │
│                    Browser / Mobile / Postman                         │
└──────────────────────────────┬─────────────────────────────────────────┘
                               │ HTTPS
┌──────────────────────────────▼─────────────────────────────────────────┐
│                         API GATEWAY (:8080)                            │
│  ┌──────────────┐ ┌────────────────┐ ┌───────────────┐ ┌───────────┐  │
│  │ JWT Validation│ │ Rate Limiter   │ │ Correlation ID│ │  Routing  │  │
│  └──────────────┘ └────────────────┘ └───────────────┘ └───────────┘  │
└──────────────────────────────┬─────────────────────────────────────────┘
                               │ HTTP (internal network)
     ┌─────────────────────────┼─────────────────────────┐
     │                         │                         │
┌────▼────┐ ┌────────┐ ┌──────▼──────┐ ┌────────┐ ┌─────▼───┐ ┌────────┐
│  Auth   │ │  User  │ │  Product   │ │ Order  │ │ Payment │ │ Review │
│ Service │ │Service │ │  Service   │ │Service │ │ Service │ │Service │
└────┬────┘ └───┬────┘ └─────┬──────┘ └───┬────┘ └────┬────┘ └───┬────┘
     │          │            │             │           │          │
     └──────────┴────────────┴──────┬──────┴───────────┴──────────┘
                                    │
     ┌──────────────────────────────┼──────────────────────────────┐
     │              INFRASTRUCTURE LAYER                           │
     │  ┌──────────┐  ┌───────┐  ┌───────┐  ┌─────────────────┐   │
     │  │PostgreSQL│  │ Redis │  │ Kafka │  │ Elasticsearch   │   │
     │  │ (6 DBs)  │  │(Cache)│  │(Events│  │ (Product Search)│   │
     │  └──────────┘  └───────┘  └───────┘  └─────────────────┘   │
     └─────────────────────────────────────────────────────────────┘
     ┌─────────────────────────────────────────────────────────────┐
     │               SPRING CLOUD LAYER                            │
     │  ┌────────────────┐         ┌──────────────────┐            │
     │  │  Eureka Server │         │  Config Server   │            │
     │  │  (Discovery)   │         │ (Centralized Cfg)│            │
     │  └────────────────┘         └──────────────────┘            │
     └─────────────────────────────────────────────────────────────┘
     ┌─────────────────────────────────────────────────────────────┐
     │               OBSERVABILITY LAYER                           │
     │  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
     │  │ Prometheus │  │  Grafana   │  │   Zipkin   │            │
     │  │ (Metrics)  │  │(Dashboards)│  │ (Tracing)  │            │
     │  └────────────┘  └────────────┘  └────────────┘            │
     └─────────────────────────────────────────────────────────────┘
```

---

## Service Responsibilities

### API Gateway
- **Single public entry point** — all client traffic enters through port 8080
- Validates JWT tokens and extracts claims (email, UUID, role)
- Injects identity headers (`X-User-UUID`, `X-User-Role`, `X-User-Email`) into downstream requests
- Per-IP rate limiting using Redis-backed counters (shared across gateway replicas)
- Generates `X-Correlation-ID` for distributed tracing
- Routes requests to services by path prefix (`/api/auth/**`, `/api/user/**`, etc.)

### Auth Service
- JWT access token generation (HS256, 15-minute expiry)
- Refresh token management with rotation — old token revoked on each refresh
- Logout via refresh token revocation
- Password recovery flow (delegates OTP generation to user-service)
- Calls user-service via Feign/REST for credential validation

### User Service
- User registration with OTP-based email verification
- Profile management (update, change password)
- Address book (CRUD, default address)
- Shopping cart and wishlist (persistent, server-side)
- Notification system (in-app, paginated, mark-as-read)
- Loyalty points program (earn, redeem, tier system: BRONZE → PLATINUM)
- Customer support tickets (create, message, admin assignment)
- Seller onboarding (business details, KYC documents)
- Admin operations (user listing, blocking, approval, search)
- Internal endpoints for cross-service user lookup (secured by `X-Internal-Secret`)

### Product Service
- Product CRUD with seller ownership and admin approval workflow (DRAFT → ACTIVE)
- Hierarchical category management (root → child categories with reordering)
- Product variants (size, color, SKU) with per-variant stock and pricing
- Multi-image support with display ordering
- Flash deals (time-limited discounts with auto-activation)
- Full-text search via Elasticsearch with Redis-cached results
- Inventory history tracking (CREATED, PURCHASED, RETURNED, ADJUSTED)
- Internal stock management endpoints (reduce/restore) for order-service

### Order Service
- Order creation with multi-seller splitting (MAIN → SUB orders per seller)
- Durable outbox publishing for `order.created` saga events
- Order lifecycle: CREATED → CONFIRMED → SHIPPED → DELIVERED
- Coupon system (percentage/flat, usage limits, expiry, per-seller restrictions)
- Return/exchange workflow with admin approval
- Shipment tracking (carrier, tracking number, location events)
- PDF invoice generation (iText) with email delivery
- Admin & seller dashboards (KPIs, revenue, order counts)
- Audit logging (full before/after state capture)
- Kafka consumer for payment completion events

### Payment Service
- Payment initiation and gateway integration (Razorpay or mock)
- Razorpay webhook processing with signature verification
- Multi-seller payment splitting (platform fee calculation)
- Idempotent processing for Kafka events and gateway callbacks (`processed_events`)
- Kafka consumer for order events, publisher for payment results
- Seller financial dashboards (earnings, pending payouts)

### Review Service
- Review CRUD with verified purchase enforcement (checks order-service)
- Star ratings (1–5) with product average rating updates
- Review images with display ordering
- Helpful/unhelpful voting system
- Admin moderation (approve/reject)
- Kafka consumer for order delivery events

---

## Infrastructure Components

### PostgreSQL 15

Six isolated databases, one per domain service:

| Database | Service | Key Tables |
|----------|---------|------------|
| `auth_db` | Auth Service | `refresh_tokens` |
| `user_db` | User Service | `users`, `addresses`, `cart_items`, `wishlist_items`, `otp_verifications`, `notifications`, `loyalty_points`, `loyalty_transactions`, `support_tickets`, `support_messages`, `seller_details` |
| `product_db` | Product Service | `products`, `categories`, `product_images`, `product_variants`, `flash_deals`, `inventory_history` |
| `order_db` | Order Service | `orders`, `order_items`, `order_event_outbox`, `coupons`, `return_requests`, `shipment_tracking`, `invoices`, `audit_logs` |
| `payment_db` | Payment Service | `payments`, `payment_splits`, `processed_events` |
| `review_db` | Review Service | `reviews`, `review_images`, `review_votes` |

All databases are created by `init.sql` on first startup.

### Redis 7

- **User Service** — cache user profiles, cart, wishlist (15-min TTL)
- **Product Service** — cache product details, search results, flash deals (10-min TTL)
- **Order Service** — cache order lookups (10-min TTL)

### Elasticsearch 8

- Used exclusively by **Product Service** for full-text product search
- Indexes product name, description, category
- Supports filters: price range, category, status
- Single-node dev setup (discovery.type=single-node)

### Apache Kafka (KRaft Mode)

- No ZooKeeper dependency — uses KRaft controller
- Topics: `order.created`, `payment.completed`, `order.confirmed`, `order.delivered`
- See [Event-Driven Architecture](#event-driven-architecture-kafka) section for details

---

## Communication Patterns

### Synchronous (REST/Feign)

| From | To | Purpose | Auth Mechanism |
|------|----|---------|----------------|
| Auth Service | User Service | Validate credentials during login | `X-Internal-Secret` header |
| Auth Service | User Service | Forward forgot/reset password | `X-Internal-Secret` header |
| Order Service | Product Service | Reduce/restore stock | `X-Internal-Secret` header |
| Review Service | Order Service | Verify purchase before review | `X-Internal-Secret` header |
| Review Service | Product Service | Update average rating | `X-Internal-Secret` header |

### Asynchronous (Kafka Events)

| Event | Producer | Consumer | Trigger |
|-------|----------|----------|---------|
| `order.created` | Order Service | Payment Service | New order placed |
| `payment.completed` | Payment Service | Order Service | Payment success/failure |
| `order.confirmed` | Order Service | — | Payment confirmed |
| `order.delivered` | Order Service | Review Service | Order marked delivered |

### Header-Based Identity Propagation

The API Gateway validates the JWT and injects these headers into every downstream request:

```
X-User-UUID: {uuid}      ← unique user identifier
X-User-Role: {role}       ← BUYER, SELLER, or ADMIN
X-User-Email: {email}     ← user's email address
X-Internal-Secret: {key}  ← service-to-service authentication
X-Correlation-ID: {id}    ← distributed tracing identifier
```

Downstream services **never parse JWTs** — they trust the gateway-injected headers. Internal calls between services use `X-Internal-Secret` for authentication.

---

## Security Architecture

### Authentication Flow

```
1. Client POSTs /api/auth/login with {email, password}
2. Gateway forwards to Auth Service (public route)
3. Auth Service calls User Service (internal) to validate credentials
4. Auth Service generates:
   - Access Token (JWT, HS256, 15min expiry)
     Claims: sub=email, uuid=userUuid, role=BUYER|SELLER|ADMIN
   - Refresh Token (UUID, stored in DB, 7-day expiry)
5. Client stores tokens, sends Authorization: Bearer {accessToken} on every request
6. Gateway validates JWT signature and expiry on every request
7. Gateway extracts claims → injects X-User-UUID, X-User-Role headers
8. Downstream service reads headers for authorization decisions
```

### Authorization Layers

| Layer | Mechanism | Details |
|-------|-----------|---------|
| **Gateway** | JWT Validation | Rejects expired/invalid tokens before reaching services |
| **Gateway** | Public Path Whitelist | Login, register, OTP, product browse bypass JWT check |
| **Service** | `@PreAuthorize` | Role-based method security (`hasRole('ADMIN')`, `hasRole('BUYER')`, etc.) |
| **Service** | Ownership Check | Users can only access their own orders, reviews, cart, etc. |
| **Service** | `X-Internal-Secret` | Service-to-service calls validated against shared secret |

### Token Refresh & Rotation

```
1. Client POSTs /api/auth/refresh?refreshToken={token}
2. Auth Service looks up refresh token in DB
3. Old refresh token is REVOKED (one-time use)
4. New access token + new refresh token issued
5. If refresh token is expired or already revoked → 401 (forces re-login)
```

### Password Security

- Passwords hashed with **BCrypt** (cost factor 10)
- Password reset uses OTP sent to email (6-digit, 10-min expiry)
- OTP marked as used after single verification
- Change password requires current password validation

---

## Database Design

### Entity Relationship Overview

#### User Service (user_db)

```
users ──────────┬── addresses (1:N)
                ├── cart_items (1:N)
                ├── wishlist_items (1:N)
                ├── otp_verifications (1:N, by email)
                ├── notifications (1:N)
                ├── loyalty_points (1:1)
                ├── loyalty_transactions (1:N)
                ├── support_tickets (1:N) ── support_messages (1:N)
                └── seller_details (1:1)
```

#### Product Service (product_db)

```
categories (self-referencing hierarchy)
     │
products ───────┬── product_images (1:N)
                ├── product_variants (1:N)
                ├── flash_deals (1:N)
                └── inventory_history (1:N)
```

#### Order Service (order_db)

```
orders ─────────┬── order_items (1:N)
                ├── return_requests (1:1)
                ├── shipment_tracking (1:N)
                └── invoices (1:1)

coupons (standalone)
audit_logs (standalone)

orders (self-referencing: parentOrderUuid for sub-orders)
```

#### Payment Service (payment_db)

```
payments ───── payment_splits (1:N, per seller)
processed_events (idempotency table)
```

#### Review Service (review_db)

```
reviews ────────┬── review_images (1:N)
                └── review_votes (1:N)
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **UUID as public identifier** | All entities use auto-generated UUIDs for external APIs; DB `id` (BIGINT) for internal joins |
| **Soft deletes** | Most entities use `isDeleted` flag instead of physical deletion |
| **Denormalized cart/wishlist** | Product name, image, price stored in cart to avoid cross-service calls on read |
| **Per-service database** | Strict data isolation per microservice — no shared tables |
| **Idempotency table** | Payment service tracks processed Kafka events to prevent duplicate processing |
| **Audit log with JSON state** | Orders store before/after JSON snapshots for compliance |

---

## Caching Strategy

| Service | Cache Target | TTL | Eviction |
|---------|-------------|-----|----------|
| User Service | User profiles | 15 min | On profile update |
| User Service | Cart contents | 15 min | On cart modification |
| Product Service | Product details | 10 min | On product update |
| Product Service | Search results | 10 min | On product change |
| Product Service | Flash deals | 10 min | On deal create/expire |
| Order Service | Order lookups | 10 min | On status change |

All caches use Redis with `spring.cache.type=redis`.

---

## Event-Driven Architecture (Kafka)

### Order → Payment → Confirmation Flow

```
┌──────────────┐     order.created      ┌─────────────────┐
│ Order Service │ ──────────────────────► │ Payment Service │
│ (Producer)    │                         │ (Consumer)      │
└──────────────┘                         └────────┬────────┘
                                                  │
                                          Processes payment
                                          (Razorpay / Mock)
                                                  │
┌──────────────┐    payment.completed    ┌────────▼────────┐
│ Order Service │ ◄────────────────────── │ Payment Service │
│ (Consumer)    │                         │ (Producer)      │
└──────┬───────┘                         └─────────────────┘
       │
  Updates order status
  (CONFIRMED or FAILED)
       │
       ▼
┌──────────────┐    order.delivered      ┌─────────────────┐
│ Order Service │ ──────────────────────► │ Review Service  │
│ (Producer)    │                         │ (Consumer)      │
└──────────────┘                         └─────────────────┘
                                          Enables review
                                          submission
```

### Idempotency

All Kafka consumers use an idempotency key (`eventId`) stored in a `processed_events` table. If a message is redelivered, the consumer checks this table and skips duplicates.

---

## Observability

### Distributed Tracing (Zipkin)

- Every request gets a `X-Correlation-ID` at the gateway
- The same ID propagates through all service-to-service calls
- Zipkin collects spans from all services via HTTP reporter
- Access: http://localhost:9411

### Metrics (Prometheus + Grafana)

- Each service exposes `/actuator/prometheus` endpoint
- Prometheus scrapes all services every 15 seconds
- Pre-configured scrape targets: all 6 microservices + gateway + eureka + config-server
- Grafana dashboards available at http://localhost:3000

### Health Checks

Every service exposes Spring Boot Actuator endpoints:
- `/actuator/health` — service health
- `/actuator/info` — service metadata
- `/actuator/prometheus` — Prometheus metrics

---

## Deployment Architecture (Docker Compose)

### Container Dependency Order

```
Level 0: postgres, redis, elasticsearch, kafka, zipkin          (infrastructure)
Level 1: eureka-server, config-server                           (spring cloud)
Level 2: auth-service, user-service, product-service,           (microservices)
         order-service, payment-service, review-service
Level 3: api-gateway                                            (gateway)
Level 4: prometheus, grafana                                    (monitoring, --profile full)
```

### Docker Network

All containers share the `ecommerce-net` bridge network. Services reference each other by container name (e.g., `postgres`, `redis`, `kafka`, `auth-service`).

### Resource Considerations

| Container | Approximate RAM |
|-----------|:-----------:|
| PostgreSQL | ~200 MB |
| Redis | ~50 MB |
| Elasticsearch | ~1 GB |
| Kafka (KRaft) | ~500 MB |
| Each Java service | ~300-500 MB |
| **Total (all containers)** | **~5-6 GB** |

Recommended minimum: **8 GB Docker memory allocation**.
