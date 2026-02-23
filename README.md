# 🛒 E-Commerce Backend (Microservices)

A production-style e-commerce backend built with **Spring Boot 3.2.x**, **Spring Cloud 2023.x**, **Java 17**, and **Docker Compose**.

This repository contains:
- API Gateway (single public entry point)
- Auth, User, Product, Order, Payment, Review microservices
- Eureka (service discovery) + Config Server (shared config)
- PostgreSQL, Redis, Kafka, Zipkin, Prometheus, Grafana

## 📘 Complete Documentation Bundle

- Deep architecture + code flow explanation: [docs/FLOW_AND_CODE_EXPLANATION.md](docs/FLOW_AND_CODE_EXPLANATION.md)
- Full line-numbered source/config reference (all included files): [docs/COMPLETE_CODE_REFERENCE.md](docs/COMPLETE_CODE_REFERENCE.md)
- Regeneration script for full reference: `scripts/generate_full_reference.sh`

---

## 📚 Table of Contents

- [1) System Overview](#1-system-overview)
- [2) Service Map and Ports](#2-service-map-and-ports)
- [3) Repository Structure](#3-repository-structure)
- [4) Architecture and Request Flow](#4-architecture-and-request-flow)
- [5) Security Model (JWT + Internal Secret)](#5-security-model-jwt--internal-secret)
- [6) Configuration and Environment Variables](#6-configuration-and-environment-variables)
- [7) API Endpoints (All Services)](#7-api-endpoints-all-services)
- [8) Event-Driven Flows (Kafka)](#8-event-driven-flows-kafka)
- [9) Database Model and Initialization](#9-database-model-and-initialization)
- [10) Local Development (Without Docker for app services)](#10-local-development-without-docker-for-app-services)
- [11) Full Docker Compose Workflow](#11-full-docker-compose-workflow)
- [12) Observability and Monitoring](#12-observability-and-monitoring)
- [13) Troubleshooting (Common Failures)](#13-troubleshooting-common-failures)
- [14) Useful Commands](#14-useful-commands)

---

## 1) System Overview

This is a **multi-module Maven monorepo** where each bounded context is an independent Spring Boot service.

High-level behavior:
1. Client calls `api-gateway`.
2. Gateway validates JWT and injects trusted identity headers.
3. Gateway forwards to downstream services.
4. Services enforce authorization with Spring Security + method-level rules.
5. Services communicate both synchronously (HTTP/Feign) and asynchronously (Kafka).

Core stack:
- Java 17
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1
- PostgreSQL 15
- Redis 7
- Apache Kafka 3.7.0 (KRaft)
- Zipkin + Prometheus + Grafana

---

## 2) Service Map and Ports

### Publicly exposed on host

| Service | Host Port | Purpose |
|---|---:|---|
| api-gateway | 8080 | Main API entry point |
| eureka-server | 8761 | Service registry UI |
| config-server | 8888 | Centralized config |
| postgres | 5432 | Database server |
| redis | 6379 | Caching / token store |
| kafka | 29092 | Kafka host listener |
| zipkin | 9411 | Tracing UI |
| prometheus | 9090 | Metrics scraping UI |
| grafana | 3000 | Dashboards |
| frontend | 3001 | Frontend container |

### Internal app services (container network)

| Service | Internal Port |
|---|---:|
| auth-service | 8080 |
| user-service | 8080 |
| product-service | 8080 |
| order-service | 8080 |
| payment-service | 8080 |
| review-service | 8080 |

---

## 3) Repository Structure

```text
ecommerce-backend/
├── pom.xml                         # Parent POM (packaging=pom)
├── docker-compose.yml              # Full stack orchestration
├── init.sql                        # Creates service databases
├── prometheus.yml                  # Prometheus scrape config
├── README.md
│
├── api-gateway/
├── auth-service/
├── user-service/
├── product-service/
├── order-service/
├── payment-service/
├── review-service/
├── eureka-server/
└── config-server/
```

Parent `pom.xml` modules:
- `user-service`
- `auth-service`
- `api-gateway`
- `product-service`
- `order-service`
- `review-service`
- `payment-service`
- `eureka-server`
- `config-server`

---

## 4) Architecture and Request Flow

```text
Client / Frontend
      │
      ▼
API Gateway (:8080)
  - JWT validation
  - identity header injection
  - internal secret header injection
      │
      ├── auth-service
      ├── user-service
      ├── product-service
      ├── order-service
      ├── payment-service
      └── review-service

Shared infra:
- Eureka (discovery)
- Config Server (shared config)
- PostgreSQL (per-service databases)
- Redis
- Kafka
- Zipkin / Prometheus / Grafana
```

Gateway route fallback (from `api-gateway/src/main/resources/application.properties`):

```properties
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://auth-service:8080
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**

spring.cloud.gateway.routes[1].id=user-service
spring.cloud.gateway.routes[1].uri=http://user-service:8080
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/user/**

spring.cloud.gateway.routes[2].id=product-service
spring.cloud.gateway.routes[2].uri=http://product-service:8080
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/product/**
```

---

## 5) Security Model (JWT + Internal Secret)

### 5.1 Gateway does authentication once

`api-gateway` validates JWT and only forwards trusted identity context.

From `SecurityConfig` (public paths + authenticated default):

```java
private static final String[] PUBLIC_PATHS = {
    "/api/auth/login",
    "/api/auth/refresh",
    "/api/user/register",
    "/api/user/verify-otp",
    "/api/user/resend-otp",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**"
};

.authorizeExchange(exchange -> exchange
    .pathMatchers(PUBLIC_PATHS).permitAll()
    .anyExchange().authenticated())
```

### 5.2 Header injection at gateway

From `InternalSecretFilterConfig`:
- strips spoofable `X-User-*` headers from incoming client requests
- extracts claims from valid Bearer token
- injects:
  - `X-User-UUID`
  - `X-User-Role`
  - `X-User-Email`
- injects `X-Internal-Secret` for inter-service trust

```java
mutated.headers(h -> {
    h.remove("X-User-UUID");
    h.remove("X-User-Role");
    h.remove("X-User-Email");
});
...
mutated.header("X-Internal-Secret", internalSecret);
```

### 5.3 Downstream authorization

Services use `@PreAuthorize` with roles:
- `BUYER`
- `SELLER`
- `ADMIN`

Example from `user-service`:

```java
@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/admin/block/{uuid}")
public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable String uuid) {
    return ResponseEntity.ok(ApiResponse.success(userService.blockUser(uuid), null));
}
```

---

## 6) Configuration and Environment Variables

Shared container env (`docker-compose.yml`):

```yaml
x-common-env: &common-env
  DB_USERNAME: postgres
  DB_PASSWORD: postgres
  JWT_SECRET: mysupersecuresecretkeythatislongenough123
  INTERNAL_SECRET: veryStrongInternalSecret123
  KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  ZIPKIN_URL: http://zipkin:9411/api/v2/spans
  EUREKA_URL: http://eureka-server:8761/eureka
  CONFIG_SERVER_URL: http://config-server:8888
  REDIS_HOST: redis
  REDIS_PORT: 6379
```

Config Server shared properties (`config-server/src/main/resources/config/application.properties`) include:
- Eureka registration defaults
- Zipkin tracing endpoint and sampling
- Actuator exposure (`health,info,metrics,prometheus,refresh`)
- Prometheus export enabled
- Shared logging pattern with `traceId` and `spanId`

> ⚠️ For real deployments, replace default secrets (`JWT_SECRET`, `INTERNAL_SECRET`) and externalize them.

---

## 7) API Endpoints (All Services)

All routes below are called through gateway base URL:

```text
http://localhost:8080
```

### 7.1 Auth Service (`/api/auth`)

From `AuthController`:

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login and receive tokens |
| POST | `/api/auth/refresh?refreshToken=...` | Public | Refresh token pair |
| POST | `/api/auth/logout?refreshToken=...` | Authenticated | Revoke/expire refresh token |

### 7.2 User Service (`/api/user`)

From `UserController`:

Public:
- `POST /api/user/register`
- `POST /api/user/verify-otp`
- `POST /api/user/resend-otp?email=...`

Authenticated user:
- `GET /api/user/me`
- `PUT /api/user/me`
- `PUT /api/user/me/change-password`

Admin:
- `PUT /api/user/admin/approve/{uuid}`
- `PUT /api/user/admin/reject/{uuid}`
- `PUT /api/user/admin/block/{uuid}`
- `PUT /api/user/admin/unblock/{uuid}`
- `GET /api/user` (paginated list)
- `DELETE /api/user/{uuid}`
- `PUT /api/user/restore/{uuid}`
- `GET /api/user/search?keyword=...`

Internal-only:
- `GET /api/user/internal/email/{email}`
- `GET /api/user/internal/uuid/{uuid}`

### 7.3 Product Service (`/api/product`)

From `ProductController`:

| Method | Path | Access |
|---|---|---|
| POST | `/api/product` | SELLER |
| PUT | `/api/product/{uuid}` | SELLER, ADMIN |
| PUT | `/api/product/admin/approve/{uuid}` | ADMIN |
| PUT | `/api/product/admin/block/{uuid}` | ADMIN |
| PUT | `/api/product/admin/unblock/{uuid}` | ADMIN |
| DELETE | `/api/product/{uuid}` | SELLER, ADMIN |
| GET | `/api/product` | Public / role-aware listing |
| GET | `/api/product/{uuid}` | Public / role-aware |
| PUT | `/api/product/internal/reduce-stock/{uuid}?quantity=` | Internal |
| PUT | `/api/product/internal/restore-stock/{uuid}?quantity=` | Internal |
| PUT | `/api/product/internal/update-rating/{uuid}?rating=` | Internal |

### 7.4 Order Service (`/api/order`)

From `OrderController`:

| Method | Path | Access |
|---|---|---|
| POST | `/api/order` | BUYER |
| GET | `/api/order` | Role-aware list |
| GET | `/api/order/seller` | SELLER |
| PUT | `/api/order/{uuid}/status?status=` | BUYER, ADMIN |
| PUT | `/api/order/internal/payment-update/{uuid}?status=` | Internal |
| GET | `/api/order/{uuid}` | Role-aware single order |

### 7.5 Payment Service (`/api/payment`)

From `PaymentController`:

| Method | Path | Access |
|---|---|---|
| POST | `/api/payment` | BUYER |
| GET | `/api/payment` | BUYER |
| GET | `/api/payment/{uuid}` | BUYER, ADMIN |
| GET | `/api/payment/order/{orderUuid}` | BUYER, ADMIN |

### 7.6 Review Service (`/api/review`)

From `ReviewController`:

| Method | Path | Access |
|---|---|---|
| POST | `/api/review` | BUYER |
| GET | `/api/review/product/{productUuid}` | Public |
| GET | `/api/review/{uuid}` | Public |
| PUT | `/api/review/{uuid}` | BUYER |
| DELETE | `/api/review/{uuid}` | BUYER, ADMIN |
| GET | `/api/review/me` | BUYER |

### 7.7 Swagger / OpenAPI

Gateway aggregates service docs:
- `http://localhost:8080/swagger-ui.html`

Configured swagger sources in gateway properties:
- `/api/auth/v3/api-docs`
- `/api/user/v3/api-docs`
- `/api/product/v3/api-docs`
- `/api/order/v3/api-docs`
- `/api/review/v3/api-docs`
- `/api/payment/v3/api-docs`

---

## 8) Event-Driven Flows (Kafka)

Implemented event flows:
- Payment processing updates order state (payment → order)
- Review events trigger product rating updates (review → product)

Infra notes:
- Kafka uses `apache/kafka:3.7.0` with KRaft mode in compose.
- Broker host port exposed on `29092` for local clients.

---

## 9) Database Model and Initialization

`init.sql` creates databases:
- `user_db`
- `auth_db`
- `order_db`
- `review_db`
- `product_db`
- `payment_db`

Current `init.sql` includes `CREATE DATABASE payment_db;` twice. On first fresh initialization this can cause a startup error if both statements execute in one run. Recommended fix is to keep only one line (or use `CREATE DATABASE ...` guarded logic).

Service persistence model:
- one database per microservice (database-per-service pattern)
- each service owns its schema and migrations

---

## 10) Local Development (Without Docker for app services)

Run infra dependencies only, then start services from IDE/terminal.

```bash
docker compose up -d postgres redis kafka zipkin eureka-server config-server
```

Build all modules:

```bash
./mvnw clean package -DskipTests
```

Run one service example:

```bash
cd product-service
./mvnw spring-boot:run
```

---

## 11) Full Docker Compose Workflow

### 11.1 Build + up

```bash
docker compose up --build
```

### 11.2 Check status

```bash
docker compose ps -a
```

### 11.3 Follow logs

```bash
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs -f product-service
```

### 11.4 Stop all services

```bash
docker compose down
```

### 11.5 Stop + remove volumes

```bash
docker compose down -v
```

---

## 12) Observability and Monitoring

- Zipkin: `http://localhost:9411`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (default `admin/admin`)

Health endpoints (examples):
- `http://localhost:8761/actuator/health`
- `http://localhost:8888/actuator/health`

Actuator/Prometheus are enabled through shared config from Config Server.

---

## 13) Troubleshooting (Common Failures)

### 13.1 `exited with code 1` during compose startup

Use this sequence:

```bash
docker compose ps -a
docker compose logs --tail=200 <service-name>
```

Most common causes in this project:
1. Infra dependency not healthy yet (Postgres/Kafka/Config/Eureka).
2. Bad or missing environment variable (`JWT_SECRET`, DB creds, etc.).
3. Database init issue (duplicate `payment_db` creation in `init.sql`).
4. Port conflicts on host (`8080`, `8761`, `8888`, etc.).

### 13.2 Clean up disk safely

```bash
docker compose down -v
docker system prune -f
docker volume prune -f
docker builder prune -f
```

For aggressive cleanup (removes unused images too):

```bash
docker system prune -a -f --volumes
```

---

## 14) Useful Commands

### Maven

```bash
# full build
./mvnw clean package

# tests only
./mvnw test

# build single module
./mvnw -pl api-gateway clean package -DskipTests
```

### Docker Compose

```bash
# rebuild and start
docker compose up --build

# restart one service
docker compose restart api-gateway

# status
docker compose ps -a
```

### Git quick update

```bash
git status
git add -A
git commit -m "docs: expand README with architecture, APIs, and operations"
git push origin master
```

---

## Notes

- Compose warns that `version` in `docker-compose.yml` is obsolete in Compose v2; it can be removed safely.
- Keep production secrets out of source-controlled compose files.
- If you want, this README can also be split into:
  - `docs/architecture.md`
  - `docs/api.md`
  - `docs/operations.md`
  for easier maintenance.
