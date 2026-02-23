# 🛒 ShopHub — E-Commerce Microservices Backend

A production-ready, cloud-native e-commerce backend built with **Spring Boot 3.2**, **Spring Cloud 2023**, and **Java 17**. The system is comprised of 6 business microservices behind an API Gateway, with full observability, distributed tracing, and an event-driven architecture powered by Apache Kafka.

A companion **React + TypeScript frontend** lives at [ecommerce-frontend](https://github.com/sourabh2308-dev/ecommerce-frontend).

---

## 📑 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Services & Ports](#-services--ports)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [API Reference](#-api-reference)
- [Security Model](#-security-model)
- [Event-Driven Flows](#-event-driven-flows)
- [Observability](#-observability)
- [Database](#-database)
- [Configuration Server](#-configuration-server)
- [Frontend](#-frontend)
- [Running Tests](#-running-tests)

---

## 🏗 Architecture Overview

```
Browser / Mobile
      │
      ▼
┌─────────────┐                     ┌───────────────────────┐
│  Frontend   │──── /api/* ────────►│     API Gateway       │ :8080
│  (React)    │                     │  (Spring Cloud GW)    │
│  :3001      │◄── response ────────│  JWT validation       │
└─────────────┘                     │  Header injection     │
                                    └──────────┬────────────┘
                                               │ X-User-UUID
                                               │ X-User-Role
                                               │ X-Internal-Secret
                          ┌────────────────────┼────────────────────┐
                          ▼                    ▼                    ▼
                    auth-service         user-service        product-service
                          │                    │                    │
                          └────────────────────┼────────────────────┘
                                               │
                          ┌────────────────────┼────────────────────┐
                          ▼                    ▼                    ▼
                    order-service       payment-service      review-service

Infrastructure:
  PostgreSQL 15 · Redis 7 · Apache Kafka 3.7 (KRaft) · Zipkin
  Prometheus · Grafana · Eureka Server · Config Server
```

The **API Gateway** is the single entry point for all client traffic.  
It validates JWTs, injects identity headers, and routes to downstream services.  
All services register with **Eureka** for service discovery.  
The **Config Server** provides shared configuration (tracing, management, logging).

---

## 📦 Services & Ports

| Service | External Port | Internal Port | Database | Description |
|---|---|---|---|---|
| `api-gateway` | **8080** | 8080 | — | Single entry point, JWT validation, routing |
| `eureka-server` | **8761** | 8761 | — | Service registry (Netflix Eureka) |
| `config-server` | **8888** | 8888 | — | Centralised Spring Cloud Config |
| `auth-service` | — | 8080 | `auth_db` | Register, login, OTP, refresh, logout |
| `user-service` | — | 8080 | `user_db` | Profile management, admin user control |
| `product-service` | — | 8080 | `product_db` | Catalogue, approval workflow, stock, ratings |
| `order-service` | — | 8080 | `order_db` | Order lifecycle, stock via Feign |
| `payment-service` | — | 8080 | `payment_db` | Payment processing and queries |
| `review-service` | — | 8080 | `review_db` | Review CRUD, pushes ratings to product |
| `frontend` | **3001** | 80 | — | React app served by Nginx |
| `postgres` | 5432 | 5432 | — | PostgreSQL 15 (6 databases) |
| `redis` | 6379 | 6379 | — | Redis 7 (cache + refresh token store) |
| `kafka` | 29092 (host) | 9092 | — | Apache Kafka 3.7 (KRaft, no Zookeeper) |
| `zipkin` | **9411** | 9411 | — | Distributed trace UI |
| `prometheus` | **9090** | 9090 | — | Metrics scraper |
| `grafana` | **3000** | 3000 | — | Metrics dashboards |

---

## ⚙️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Microservices | Spring Cloud 2023.0.1 |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Netflix Eureka |
| Config Management | Spring Cloud Config Server |
| Security | Spring Security + JWT (JJWT), method-level `@PreAuthorize` |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Caching | Redis 7 (Spring Cache abstraction) |
| Messaging | Apache Kafka 3.7 (KRaft — no Zookeeper) |
| Migrations | Flyway |
| Inter-service HTTP | OpenFeign + Resilience4j circuit breakers |
| Distributed Tracing | Micrometer Tracing + Zipkin |
| Metrics | Micrometer + Prometheus + Grafana |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven (multi-module, 10 modules) |
| Containers | Docker + Docker Compose v2 |
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS 4 |

---

## 📁 Project Structure

```
ecommerce-backend/
├── pom.xml                        # Parent POM (10 modules)
├── docker-compose.yml             # Full-stack orchestration (16 services)
├── init.sql                       # PostgreSQL DB initialisation
├── prometheus.yml                 # Prometheus scrape targets
├── grafana/provisioning/          # Grafana auto-provisioned datasource
│
├── config-server/                 # Spring Cloud Config Server
│   └── src/main/resources/config/
│       └── application.properties # Shared config for all services
│
├── eureka-server/                 # Netflix Eureka Server
├── api-gateway/                   # Spring Cloud Gateway + JWT filter
│
├── auth-service/
├── user-service/
├── product-service/               # Includes V2 migration: add image_url
├── order-service/
├── payment-service/
└── review-service/
```

Each service follows the same internal structure:

```
{service}/src/main/java/com/sourabh/{service}/
├── config/          # Security, CORS, Redis, OpenAPI, Feign config
├── controller/      # REST controllers
├── dto/             # Request / Response DTOs
├── entity/          # JPA entities
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── filter/          # CorrelationId, InternalSecret request filters
├── kafka/           # Producers, Consumers, Event model classes
├── repository/      # Spring Data JPA repositories
└── service/         # Service interfaces + implementations
```

---

## ✅ Prerequisites

- **Java 17+**
- **Maven 3.9+** (or the bundled `./mvnw` wrapper)
- **Docker** and **Docker Compose** v2

---

## 🚀 Getting Started

### 1 — Clone

```bash
git clone https://github.com/sourabh2308-dev/ecommerce-backend.git
cd ecommerce-backend
```

### 2 — Build all JARs

```bash
./mvnw clean package -DskipTests
```

### 3 — Start the full stack

```bash
docker compose up --build
```

Docker Compose enforces startup order via health-check `depends_on` conditions:

```
PostgreSQL + Redis + Kafka + Zipkin
        ↓
  Eureka Server
        ↓
  Config Server
        ↓
  auth · user · product · order · payment · review services
        ↓
  API Gateway  →  Frontend
        ↓
  Prometheus  →  Grafana
```

### 4 — Verify

| URL | Description |
|---|---|
| `http://localhost:3001` | React frontend |
| `http://localhost:8080` | API Gateway (all API requests go here) |
| `http://localhost:8761` | Eureka service registry dashboard |
| `http://localhost:8888/actuator/health` | Config server health |
| `http://localhost:9411` | Zipkin distributed traces |
| `http://localhost:9090` | Prometheus metrics explorer |
| `http://localhost:3000` | Grafana dashboards (login: `admin` / `admin`) |

### 5 — Local Development (single service)

Start only the infrastructure, then run any service from your IDE or terminal:

```bash
# Infrastructure only
docker compose up postgres redis kafka zipkin eureka-server config-server -d

# Run a service
cd product-service
./mvnw spring-boot:run
```

---

## 🔑 Environment Variables

All secrets live in `docker-compose.yml` under the `x-common-env` YAML anchor and are injected into every service container. Override them via a `.env` file at the repo root.

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_SECRET` | `mysupersecurejwtsecretkey...` | HMAC-SHA256 signing key (≥ 32 chars) |
| `INTERNAL_SECRET` | `veryStrongInternalSecret...` | Shared header for service-to-service calls |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker address |
| `ZIPKIN_URL` | `http://zipkin:9411/api/v2/spans` | Zipkin span reporting endpoint |
| `EUREKA_URL` | `http://eureka-server:8761/eureka` | Eureka registration URL |
| `CONFIG_SERVER_URL` | `http://config-server:8888` | Spring Cloud Config URL |
| `REDIS_HOST` | `redis` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |

> ⚠️ **Always change `JWT_SECRET` and `INTERNAL_SECRET` before any public deployment.**

---

## 📡 API Reference

All routes go through the API Gateway at `http://localhost:8080`.

The gateway strips the `Authorization` header and injects:
- `X-User-UUID` — authenticated user's UUID
- `X-User-Role` — `BUYER`, `SELLER`, or `ADMIN`
- `X-Internal-Secret` — consumed by internal-only endpoints

---

### Auth  `/api/auth`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new user (BUYER or SELLER role) |
| `POST` | `/api/auth/verify-otp` | Public | Verify email OTP sent on registration |
| `POST` | `/api/auth/resend-otp` | Public | Resend OTP to registered email |
| `POST` | `/api/auth/login` | Public | Login → returns `accessToken` + `refreshToken` |
| `POST` | `/api/auth/refresh` | Public | Rotate refresh token, get new access token |
| `POST` | `/api/auth/logout` | Bearer | Revoke refresh token (stored in Redis) |

**Token lifetimes:**  
Access token — 15 minutes | Refresh token — 7 days (stored in Redis, rotated on use)

---

### User  `/api/user`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/user/me` | Bearer | Get own profile |
| `PUT` | `/api/user/me` | Bearer | Update own profile |
| `PUT` | `/api/user/me/change-password` | Bearer | Change password |
| `GET` | `/api/user/{uuid}` | ADMIN | Get any user by UUID |
| `GET` | `/api/user/all` | ADMIN | Paginated list of all users |
| `PUT` | `/api/user/block/{uuid}` | ADMIN | Block a user account |
| `PUT` | `/api/user/unblock/{uuid}` | ADMIN | Unblock a user account |
| `DELETE` | `/api/user/{uuid}` | ADMIN | Soft-delete a user |

---

### Product  `/api/product`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/product` | Public | Paginated product list (BUYER sees ACTIVE only) |
| `GET` | `/api/product/{uuid}` | Public | Get product detail |
| `POST` | `/api/product` | SELLER | Create product (starts as `DRAFT`) |
| `PUT` | `/api/product/{uuid}` | SELLER/ADMIN | Update product fields including `imageUrl` |
| `DELETE` | `/api/product/{uuid}` | SELLER/ADMIN | Soft-delete product |
| `PUT` | `/api/product/admin/approve/{uuid}` | ADMIN | Approve product (`DRAFT` → `ACTIVE`) |
| `PUT` | `/api/product/admin/block/{uuid}` | ADMIN | Block a product |
| `PUT` | `/api/product/admin/unblock/{uuid}` | ADMIN | Unblock product (`BLOCKED` → `DRAFT`) |

**Product statuses:** `DRAFT` → `ACTIVE` ↔ `BLOCKED`, `OUT_OF_STOCK` (auto when stock = 0)

**Query params for `GET /api/product`:** `page`, `size`, `sortBy`, `direction`, `keyword`

**Internal endpoints** (require `X-Internal-Secret` header, called by other services):
- `PUT /api/product/internal/reduce-stock/{uuid}`
- `PUT /api/product/internal/restore-stock/{uuid}`
- `PUT /api/product/internal/update-rating/{uuid}`

---

### Order  `/api/order`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/order` | BUYER | Place an order (reduces stock via Feign) |
| `GET` | `/api/order/{uuid}` | Bearer | Get order (ownership enforced for BUYER) |
| `GET` | `/api/order/my-orders` | BUYER | List own orders |
| `GET` | `/api/order/seller/orders` | SELLER | List orders containing seller's products |
| `PUT` | `/api/order/{uuid}/status` | SELLER/ADMIN | Advance order status |
| `PUT` | `/api/order/{uuid}/cancel` | BUYER | Cancel order (restores stock) |

**Order statuses:** `PENDING` → `PROCESSING` → `SHIPPED` → `DELIVERED` | `CANCELLED`

---

### Payment  `/api/payment`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/payment` | BUYER | Initiate payment for an order |
| `GET` | `/api/payment` | BUYER/ADMIN | List payments (BUYER sees own only) |
| `GET` | `/api/payment/{uuid}` | BUYER/ADMIN | Get payment by UUID |
| `GET` | `/api/payment/order/{orderUuid}` | BUYER/ADMIN | Get payment by order UUID |

**Payment statuses:** `PENDING`, `SUCCESS`, `FAILED`

---

### Review  `/api/review`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/review` | BUYER | Submit a review (linked to a completed order) |
| `GET` | `/api/review/product/{productUuid}` | Public | List reviews for a product |
| `GET` | `/api/review/{uuid}` | Public | Get single review |
| `PUT` | `/api/review/{uuid}` | BUYER (owner) | Update own review |
| `DELETE` | `/api/review/{uuid}` | BUYER (owner) | Delete own review |
| `GET` | `/api/review/me` | BUYER | Get all own reviews |

After any review create/update/delete, a Kafka event triggers the product-service to recalculate the product's average rating.

---

## 🔒 Security Model

### JWT Authentication Flow

```
1. Client          → POST /api/auth/login
2. auth-service    → validates credentials, issues accessToken (15 min) + refreshToken (7 d)
3. Client attaches → Authorization: Bearer <accessToken>
4. API Gateway     → validates JWT signature & expiry
5. API Gateway     → injects X-User-UUID, X-User-Role, strips Authorization header
6. Downstream      → reads headers, builds SecurityContext (no JWT dependency)
7. On 401          → client calls POST /api/auth/refresh
```

### Role-Based Access Control

Roles are encoded in the JWT and forwarded via `X-User-Role`. Each service applies `@PreAuthorize` at the method level:

| Role | Capabilities |
|---|---|
| **BUYER** | Browse products, place orders, make payments, write reviews |
| **SELLER** | Manage own products (CRUD), view own orders and payments |
| **ADMIN** | Full system access: approve/block products, manage all users, view all orders/payments |

### Inter-Service Security

Internal Feign calls (e.g. order-service → product-service for stock) include an `X-Internal-Secret` header. Each service validates this via `InternalSecretFilter` before processing internal-only endpoints.

---

## 📨 Event-Driven Flows

### Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `payment-events` | payment-service | order-service |
| `review-submitted-events` | review-service | product-service |

### Payment → Order Status Update

```
BUYER places order
  └─► order-service publishes OrderCreatedEvent (Kafka)
        └─► payment-service consumes → creates PENDING payment
              └─► payment-service publishes PaymentProcessedEvent (Kafka)
                    └─► order-service consumes → updates order to PROCESSING
```

### Stock Management (Synchronous via Feign)

```
Order confirmed  ──► order-service calls product-service → /internal/reduce-stock
Order cancelled  ──► order-service calls product-service → /internal/restore-stock
Stock drops to 0 ──► product-service auto-sets status = OUT_OF_STOCK
Stock restored   ──► product-service auto-sets status = ACTIVE
```

### Review → Rating Update

```
Buyer submits/edits/deletes review
  └─► review-service publishes ReviewSubmittedEvent (Kafka)
        └─► product-service consumes → recalculates averageRating + totalReviews
```

---

## 📊 Observability

### Distributed Tracing — Zipkin

Every service propagates W3C `traceId` / `spanId` via Micrometer + Brave.  
View full end-to-end request traces at `http://localhost:9411`.

### Metrics — Prometheus + Grafana

- Each service exposes `/actuator/prometheus`
- Prometheus scrapes all services (configured in `prometheus.yml`)
- Grafana at `http://localhost:3000` (login: `admin` / `admin`) has a pre-provisioned Prometheus datasource

### Structured Logging

All services use `logback-spring.xml` with a pattern that includes `traceId`, `spanId`, and service name for easy log correlation (compatible with ELK / Loki).

### Correlation IDs

`CorrelationIdFilter` on every service generates/propagates an `X-Correlation-ID` header through the full request chain.

---

## 🗄 Database

**One PostgreSQL database per service** (database-per-service pattern for isolation):

| Service | Database |
|---|---|
| auth-service | `auth_db` |
| user-service | `user_db` |
| product-service | `product_db` |
| order-service | `order_db` |
| payment-service | `payment_db` |
| review-service | `review_db` |

All 6 databases are created by `init.sql` when the PostgreSQL container first starts.

**Schema lifecycle:**  
Managed by **Flyway** (`spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=validate`).  
Migration scripts live at `src/main/resources/db/migration/V{n}__{description}.sql`.

Notable migrations:
- `product-service/V2__add_image_url.sql` — `ALTER TABLE product ADD COLUMN image_url VARCHAR(1000)`

---

## ⚙️ Configuration Server

Shared configuration is in `config-server/src/main/resources/config/application.properties` and is served to all services at startup.

**Shared properties include:**
- Eureka client registration
- Micrometer / Zipkin distributed tracing
- Management endpoint exposure (health, prometheus, info)
- Logging pattern with `traceId`, `spanId`, `serviceName`

Each service's local `application.properties` contains only service-specific config:  
datasource URL, Kafka topics, Redis cache TTLs, Feign client URLs, server port, etc.

Services include `spring.config.import=optional:configserver:${CONFIG_SERVER_URL}` to pull shared properties at boot.

---

## 🖥 Frontend

The React frontend is maintained separately and runs on **port 3001** when started via Docker Compose.

| Detail | Value |
|---|---|
| Technology | React 18 + TypeScript + Vite + Tailwind CSS 4 |
| State management | Zustand (auth state, cart) |
| Server state | TanStack React Query v5 |
| HTTP client | Axios with JWT interceptor + automatic token refresh |
| Forms | React Hook Form + Zod validation |
| UI | Lucide React icons, react-hot-toast notifications |
| Served by | Nginx (in Docker), proxies `/api/*` → `api-gateway:8080` |

**Pages by role:**

| Role | Pages |
|---|---|
| Guest | Products browse, Product detail, Login, Register |
| BUYER | + Cart, Checkout, My Orders, Order Detail, Reviews |
| SELLER | Seller Products (image URL, price, stock), Seller Orders |
| ADMIN | Admin Users (block/unblock), Admin Products (approve/block), Admin Orders |

Product images are stored as URLs (CDN / S3 link) set by the seller in the product form.

---

## 🧪 Running Tests

```bash
# Run all tests across all modules
./mvnw test

# Run tests in a single service
./mvnw -pl product-service test

# Build without running tests
./mvnw clean package -DskipTests
```

All **54 tests pass** across the 10 Maven modules. Integration tests use an H2 in-memory database; Kafka listener auto-startup is disabled during tests.

---

## 🐳 Docker Images

Each service has its own `dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build a single image manually:

```bash
./mvnw -pl product-service clean package -DskipTests
docker build -f product-service/dockerfile -t shophub/product-service:latest .
```

Build and start the entire stack:

```bash
docker compose up --build
```

---

## 📜 License

This project is for educational and portfolio purposes.
