# 🛒 SourHub (Microservices) - E-Commerce Platform

A **production-ready** e-commerce backend built with **Spring Boot 3.2.x**, **Spring Cloud 2023.x**, **Java 17**, and **Docker Compose**.

## 🎉 Latest Release - February 2026

This repository now includes comprehensive features for a full-featured e-commerce platform:

### Core Platform
- API Gateway (single public entry point with JWT validation)
- 6 Microservices: Auth, User, Product, Order, Payment, Review
- Service Discovery (Eureka) + Centralized Config (Config Server)
- PostgreSQL, Redis, Kafka, Elasticsearch
- Observability: Zipkin, Prometheus, Grafana

### New Features (2026 Update)
- **🔐 Password Recovery** - Forgot/reset password flow
- **📍 Address Management** - Multiple shipping addresses
- **🛒 Shopping Cart & Wishlist** - Persistent cart with item management
- **🔔 Notifications** - Real-time order and payment updates
- **⭐ Loyalty Program** - Points earning and redemption
- **💬 Support Tickets** - Multi-channel customer support
- **🎟️ Coupons & Discounts** - Flexible discount campaigns
- **↩️ Returns & Exchanges** - Complete return workflow
- **📦 Shipment Tracking** - Real-time delivery tracking
- **📄 Invoice Generation** - PDF invoice downloads, email delivery on demand, and automatic invoice email when an order is marked delivered
- **📊 Business Dashboards** - Admin and seller analytics
- **📝 Audit Logs** - Complete compliance trail
- **🏷️ Product Categories** - Hierarchical catalog organization
- **🖼️ Multi-Image Products** - Product image galleries
- **📐 Product Variants** - Size, color, style variations
- **⚡ Flash Deals** - Time-limited promotions
- **📈 Inventory Tracking** - Stock movement history
- **🌟 Enhanced Reviews** - Images, voting, verified purchases

## 📘 Complete Documentation Bundle

**Start Here** → [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) (Master navigation guide)

### Core Documentation (Read in This Order)
1. **[ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md)** (70KB)
   - Complete system architecture with diagrams
   - 6 microservices breakdown and responsibilities
   - Core flows (registration, order saga, payment, reviews)
   - Database design, security layers, testing strategy
   - **Best for**: Understanding the ENTIRE system

2. **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** (120KB)
   - Complete REST API reference for all 6 services
   - 25+ endpoints with request/response examples
   - Authorization rules, error handling, HTTP status codes
   - Complete workflow examples (buyer journey, seller story)
   - **Best for**: Using the APIs, integration planning

3. **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** (35KB)
   - Master index and navigation for all documentation
   - Quick links for different personas (devs, PMs, DevOps)
   - Documentation coverage and maintenance guide
   - **Best for**: Finding what you need, understanding what's documented

### Additional Resources
- **[COMPLETION_SUMMARY.md](COMPLETION_SUMMARY.md)** - Summary of all documentation work completed
- **In-Code Comments** - 2,700+ lines across 6 core files:
  - Auth Service (AuthServiceImpl, AuthController, JwtUtil)
  - Order Service (OrderServiceImpl, OrderController)
  - Detailed explanations of business logic and flows
- **Enhanced Tests** - 18 new test methods with edge cases
  - PaymentServiceImplTest, OrderServiceImplTest
  - ReviewServiceImplTest, OrderRepositoryIntegrationTest

---

## 📚 Table of Contents

**New Developers?** Start with [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) (1-2 hour read)

- [0) Complete Documentation Index](#0-complete-documentation-index)
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

## 0) Complete Documentation Index

### 🎯 Quick Start by Role

**👨‍💻 Backend Developer**
1. Read: [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) → Understand system design
2. Read: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) → Learn all endpoints  
3. Check: In-code comments in OrderServiceImpl, AuthServiceImpl for patterns
4. Run: Tests to see edge cases (OrderServiceImplTest, ReviewServiceImplTest)
5. Modify: Follow the patterns documented in each file

**🔍 DevOps/Infrastructure**
1. Check: `docker-compose.yml` (service definitions)
2. Read: Section [11) Full Docker Compose Workflow](#11-full-docker-compose-workflow)
3. Read: [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) → Technology Stack section
4. Monitor: Prometheus (9090), Grafana (3000), Zipkin (9411)

**🎨 Frontend Developer / API Consumer**
1. Read: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) → Request/Response formats
2. Search: Find your endpoint (POST /api/order, GET /api/product, etc.)
3. Copy: Request examples, test with cURL/Postman
4. Check: Error codes, validation rules, authorization

**👔 Product Manager / Business Analyst**
1. Read: [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) → "Core Flows" section
2. Read: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) → "Workflow Examples"
3. Review: User journeys (buyer, seller)
4. Check: Understand who can do what (authorization rules)

**📚 New Team Member (Onboarding)**
1. Start: [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) (you are here!)
2. Deep Dive: [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) (2 hours)
3. Reference: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) (1 hour)
4. Practice: Run tests, try API calls with curl
5. Code: Follow patterns from auth-service and order-service

### 📊 Documentation Statistics

| Content | Details |
|---------|---------|
| **Architecture Guide** | 70KB, complete system design with diagrams |
| **API Documentation** | 120KB, 25+ endpoints with examples |
| **Code Comments** | 2,700+ lines across 6 core files |
| **Test Cases** | 18 new edge case tests |
| **Git Commits** | 4 clean, well-documented commits |
| **Total Time to Read All** | ~4 hours for new developer |

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

**⭐ For complete API reference, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md)**

All routes below are called through gateway base URL:

```text
http://localhost:8080
```

### Quick Endpoint Reference

| Service | Endpoints | Details |
|---------|-----------|---------|
| **Auth** | login, refresh, logout, forgot-password, reset-password (OTP-based) | See [API_DOCUMENTATION.md](API_DOCUMENTATION.md#-auth-service---jwt-management) |
| **User** | register, verify-otp, profile, addresses, cart, wishlist, notifications, loyalty, support | See [API_DOCUMENTATION.md](API_DOCUMENTATION.md#-user-service---profile--registration) |
| **Product** | create, search, categories, images, variants, flash-deals, inventory | See [API_DOCUMENTATION.md](API_DOCUMENTATION.md#-product-service---catalog-management) |
| **Order** | create, list, coupons, returns, tracking, invoice, audit, dashboards | See [API_DOCUMENTATION.md](API_DOCUMENTATION.md#-order-service---order-processing) |
| **Payment** | history, dashboards (seller, admin) | See [API_DOCUMENTATION.md](API_DOCUMENTATION.md#-payment-service---payment-processing) |
| **Review** | create, list, update, delete, vote, images | See [API_DOCUMENTATION.md](API_DOCUMENTATION.md#-review-service---product-reviews) |

### Example: Order Creation (Shows Saga Pattern)

```bash
# 1. Create order (saga triggered)
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productUuid": "prod-1", "quantity": 2}],
    "shippingName": "John", "shippingAddress": "123 St", ...
  }' 

# Returns: orderUuid, paymentStatus=PENDING

# 2. Poll for payment completion
curl http://localhost:8080/api/order/{orderUuid} \
  -H "Authorization: Bearer {accessToken}"

# Saga flow (behind scenes):
# - order-service creates Order
# - Publishes order.created Kafka event
# - payment-service consumes, processes payment
# - Publishes payment.completed event
# - order-service updates paymentStatus
```

**See [ARCHITECTURE_GUIDE.md → Order Creation Flow](ARCHITECTURE_GUIDE.md#-order-creation-flow-synchronous--asynchronous-saga) for complete saga diagram**

### 7.1 Auth Service (`/api/auth`)

From `AuthController`:

From `AuthController`:

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login and receive tokens |
| POST | `/api/auth/refresh?refreshToken=...` | Public | Refresh token pair |
| POST | `/api/auth/logout?refreshToken=...` | Authenticated | Revoke/expire refresh token |

### 7.2 User Service (`/api/user`)

From `UserController`:

Public:
- `POST /api/user/register` (emails from deleted accounts may be reused)
- `POST /api/user/verify-otp`
- `POST /api/user/resend-otp?email=...`
- `POST /api/auth/forgot-password`
- `POST /api/user/reset-password`

Authenticated user:
- `GET /api/user/me`
- `PUT /api/user/me`
- `PUT /api/user/me/change-password`

Addresses (BUYER):
- `GET /api/user/addresses` - List all addresses
- `POST /api/user/addresses` - Add new address
- `PUT /api/user/addresses/{uuid}` - Update address
- `DELETE /api/user/addresses/{uuid}` - Remove address
- `PUT /api/user/addresses/{uuid}/default` - Set as default

Cart & Wishlist (BUYER):
- `GET /api/user/cart` - Get cart with items and total
- `POST /api/user/cart/add` - Add product to cart
- `PUT /api/user/cart/update` - Update quantity
- `DELETE /api/user/cart/remove/{productUuid}` - Remove item
- `DELETE /api/user/cart/clear` - Clear entire cart
- `GET /api/user/wishlist` - List wishlist items
- `POST /api/user/wishlist/add` - Add to wishlist
- `DELETE /api/user/wishlist/remove/{productUuid}` - Remove from wishlist

Notifications & Loyalty (BUYER):
- `GET /api/user/notifications` - List all notifications
- `GET /api/user/notifications/unread-count` - Unread count
- `PUT /api/user/notifications/{uuid}/read` - Mark as read
- `PUT /api/user/notifications/read-all` - Mark all as read
- `GET /api/user/loyalty/balance` - Get loyalty points balance
- `GET /api/user/loyalty/history` - Points transaction history
- `POST /api/user/loyalty/redeem` - Redeem points

Support Tickets:
- `GET /api/user/support/my-tickets` - Buyer's tickets
- `POST /api/user/support/tickets` - Create new ticket
- `GET /api/user/support/tickets/{uuid}` - Get ticket details
- `POST /api/user/support/tickets/{uuid}/messages` - Add message
- `PUT /api/user/support/tickets/{uuid}/close` - Close ticket
- `GET /api/user/support/admin/tickets` - All tickets (ADMIN)
- `PUT /api/user/support/admin/tickets/{uuid}/assign` - Assign to admin
- `PUT /api/user/support/admin/tickets/{uuid}/status` - Update status

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
| GET | `/api/product/search?query=...` | Public (Elasticsearch) |
| GET | `/api/product/{uuid}/recommendations` | Public |

Categories:
- `GET /api/product/categories` - List all categories
- `GET /api/product/categories/tree` - Hierarchical tree
- `GET /api/product/categories/{uuid}` - Single category
- `POST /api/product/categories` - Create category (ADMIN)
- `PUT /api/product/categories/{uuid}` - Update category (ADMIN)
- `DELETE /api/product/categories/{uuid}` - Delete category (ADMIN)

Product Images:
- `GET /api/product/{productUuid}/images` - List images
- `POST /api/product/{productUuid}/images` - Add image (SELLER)
- `PUT /api/product/{productUuid}/images/{imageId}` - Update (SELLER)
- `DELETE /api/product/{productUuid}/images/{imageId}` - Remove (SELLER)

Product Variants (Size, Color, etc.):
- `GET /api/product/{productUuid}/variants` - List variants
- `GET /api/product/{productUuid}/variants/{variantUuid}` - Get variant
- `POST /api/product/{productUuid}/variants` - Create variant (SELLER)
- `PUT /api/product/{productUuid}/variants/{variantUuid}` - Update (SELLER)
- `DELETE /api/product/{productUuid}/variants/{variantUuid}` - Delete (SELLER)

Flash Deals:
- `GET /api/product/flash-deals/active` - Active flash deals
- `GET /api/product/flash-deals` - All deals (ADMIN)
- `GET /api/product/flash-deals/{uuid}` - Single deal
- `POST /api/product/flash-deals` - Create deal (ADMIN)
- `PUT /api/product/flash-deals/{uuid}` - Update deal (ADMIN)
- `DELETE /api/product/flash-deals/{uuid}` - Delete deal (ADMIN)

Inventory Management:
- `GET /api/product/inventory/{productUuid}/movements` - Stock history
- `POST /api/product/inventory/{productUuid}/add` - Add stock (SELLER)
- `POST /api/product/inventory/{productUuid}/remove` - Remove stock (SELLER)
- `PUT /api/product/inventory/{productUuid}/set` - Set stock (SELLER)

Internal:
- `PUT /api/product/internal/reduce-stock/{uuid}?quantity=` | Internal |
- `PUT /api/product/internal/restore-stock/{uuid}?quantity=` | Internal |
- `PUT /api/product/internal/update-rating/{uuid}?rating=` | Internal |

### 7.4 Order Service (`/api/order`)

From `OrderController`:

| Method | Path | Access |
|---|---|---|
| POST | `/api/order` | BUYER |
| GET | `/api/order` | Role-aware list |
| GET | `/api/order/seller` | SELLER |
| GET | `/api/order/{uuid}` | Role-aware single order |
| PUT | `/api/order/{uuid}/status?status=` | BUYER, ADMIN |
| GET | `/api/order/{orderUuid}/invoice` | BUYER, SELLER, ADMIN |

Coupons:
- `POST /api/order/coupons` - Create coupon (ADMIN, SELLER)
- `POST /api/order/coupons/validate` - Validate coupon (BUYER)
- `GET /api/order/coupons` - List all coupons (ADMIN)
- `GET /api/order/coupons/{code}` - Get coupon (ADMIN)
- `DELETE /api/order/coupons/{code}` - Deactivate coupon (ADMIN)

Returns & Exchanges:
- `POST /api/order/returns` - Request return/exchange (BUYER)
- `GET /api/order/returns/my-returns` - Buyer's returns
- `GET /api/order/returns/{uuid}` - Get return details
- `GET /api/order/returns` - All returns (ADMIN)
- `PUT /api/order/returns/{uuid}/approve` - Approve return (ADMIN)
- `PUT /api/order/returns/{uuid}/reject` - Reject return (ADMIN)
- `PUT /api/order/returns/{uuid}/status` - Update status (ADMIN, SELLER)

Shipment Tracking:
- `POST /api/order/tracking` - Add tracking event (SELLER, ADMIN)
- `GET /api/order/tracking/{orderUuid}` - Get tracking history

Dashboards:
- `GET /api/order/dashboard/admin` - Admin metrics (ADMIN)
- `GET /api/order/dashboard/seller` - Seller metrics (SELLER)

Audit Logs:
- `GET /api/order/audit` - All audit logs (ADMIN)
- `GET /api/order/audit/actor/{actorUuid}` - Actor's actions (ADMIN)
- `GET /api/order/audit/order/{orderUuid}` - Order audit trail (ADMIN)

Internal:
- `PUT /api/order/internal/payment-update/{uuid}?status=` | Internal |

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

Review Images:
- `POST /api/review/{reviewUuid}/images` - Add image to review (BUYER)

Review Voting (Helpful/Not Helpful):
- `POST /api/review/{reviewUuid}/vote` - Vote on review (BUYER)
- `GET /api/review/{reviewUuid}/votes/summary` - Get vote counts

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

## 7.8) New Feature Highlights (2026 Update)

This release includes major feature expansions across all services:

### 7.8.1 User Experience Enhancements

**Password Recovery**
- Forgot password flow with email-based token
- Secure password reset with expiring tokens
- Email notifications via Kafka events

**Address Management**
- Multiple shipping addresses per user
- Default address selection
- Address validation and formatting

**Shopping Cart & Wishlist**
- Persistent cart with quantity management
- Move items between cart and wishlist
- Cart totals and item count
- Clear cart functionality

**Notifications System**
- Real-time order status notifications
- Payment confirmations and alerts
- Return/refund updates
- Promotional notifications
- Unread count and mark-as-read

**Loyalty Program**
- Points earned on orders and reviews
- Points redemption for discounts
- Transaction history and balance tracking
- Expiry management

**Support Ticket System**
- Multi-category ticket creation
- Message threading
- Status tracking (OPEN, IN_PROGRESS, RESOLVED, CLOSED)
- Admin assignment and resolution
- Order context linking

### 7.8.2 Order Management Expansion

**Coupon & Discount System**
- PERCENTAGE and FLAT discount types
- Usage limit controls (global + per-user)
- Minimum order amount requirements
- Validity period enforcement
- Seller-specific and platform-wide coupons
- Automatic validation before checkout

**Returns & Exchanges**
- REFUND and EXCHANGE request types
- Admin approval/rejection workflow
- Status progression (REQUESTED → PICKUP → RECEIVED → PROCESSED)
- Refund amount customization
- Admin notes and buyer reasons
- Order status synchronization

**Shipment Tracking**
- Multi-event tracking timeline
- Location updates
- Carrier and tracking number support
- Real-time status changes
- Delivery estimates

**Invoice Generation**
- PDF invoice generation using iText
- Professional formatting with company branding
- Itemized breakdown
- Tax and discount calculations
- Download for buyers, sellers, and admins

**Order Auditing**
- Comprehensive audit trail for all order changes
- Actor (user) tracking
- Before/after state capture (JSON)
- Timestamp recording
- Admin-only access for compliance

**Business Dashboards**
- **Admin Dashboard**: Platform-wide order metrics, revenue, returns
- **Seller Dashboard**: Seller-specific orders, deliveries, earnings

### 7.8.3 Product Catalog Improvements

**Category Management**
- Hierarchical category tree
- Parent-child relationships
- Display ordering
- Category images
- Active/inactive status

**Multi-Image Support**
- Multiple product images per product
- Display order management
- Alt text for accessibility
- Image URL storage

**Product Variants**
- Size, color, style variations
- Individual SKU tracking
- Stock management per variant
- Price overrides
- Active/inactive variants

**Flash Deals**
- Time-limited discount campaigns
- Percentage-based discounts
- Start and end time enforcement
- Active deal filtering
- Admin-only management

**Inventory Tracking**
- Stock movement history
- Add/remove stock operations
- Stock level adjustments
- Reference tracking (order UUID, manual adjustment)
- Audit trail for inventory changes

**Smart Recommendations**
- Product similarity recommendations
- Based on category and attributes
- Elasticsearch-powered search

### 7.8.4 Review Enhancements

**Review Images**
- Multiple images per review
- URL-based storage
- Display in review list

**Verified Purchase Badge**
- Automatic verification for order-based reviews
- Trust indicator for buyers

**Review Voting**
- Helpful/Not Helpful votes
- Vote count display
- One vote per user per review
- Aggregate vote summaries

---

## 8) Event-Driven Flows (Kafka)

Implemented event flows:

### Core Saga Flows
- **Payment processing updates order state** (`payment → order`)
  - Topic: `payment.completed`
  - order-service updates `paymentStatus` when payment succeeds/fails

- **Review events trigger product rating updates** (`review → product`)
  - Topic: `review.submitted`
  - product-service recalculates average rating

### New Event Flows (2026)

- **Order status notifications** (`order → user`)
  - Topic: `order.status.changed`
  - user-service creates notifications for buyers
  - Events: ORDER_PLACED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED, ORDER_CANCELLED

- **Return/refund notifications** (`order → user`)
  - Topic: `order.return.status.changed`
  - Events: RETURN_APPROVED, RETURN_REJECTED, REFUND_PROCESSED

- **Payment notifications** (`payment → user`)
  - Topic: `payment.status.changed`
  - Events: PAYMENT_SUCCESS, PAYMENT_FAILED

- **Loyalty points rewards** (`review → user`, `order → user`)
  - Topics: `review.submitted`, `order.delivered`
  - user-service awards loyalty points for orders and reviews

- **Forgot password emails** (`user → notification`)
  - Topic: `user.password.reset.requested`
  - Triggers email notification with reset token

Infra notes:
- Kafka uses `apache/kafka:3.7.0` with KRaft mode in compose.
- Broker host port exposed on `29092` for local clients.
- All events use JSON serialization
- Dead letter queues configured for failed event processing

---

## 9) Database Model and Initialization

`init.sql` creates databases:
- `user_db` - Users, addresses, cart, wishlist, notifications, loyalty points, support tickets
- `auth_db` - Refresh tokens, password reset tokens
- `order_db` - Orders, items, coupons, returns, tracking events, audit logs
- `review_db` - Reviews, review images, review votes  
- `product_db` - Products, categories, images, variants, flash deals, stock movements
- `payment_db` - Payments, payment splits

Current `init.sql` includes `CREATE DATABASE payment_db;` twice. On first fresh initialization this can cause a startup error if both statements execute in one run. Recommended fix is to keep only one line (or use `CREATE DATABASE ...` guarded logic).

Service persistence model:
- one database per microservice (database-per-service pattern)
- each service owns its schema and migrations
- Flyway handles schema versioning

### Key New Tables (2026 Update)

**user_db:**
- `address` - Multiple shipping addresses per user
- `cart_item` - Shopping cart persistence
- `wishlist_item` - Wishlist management
- `notification` - User notifications (order updates, promotions)
- `loyalty_points` - Points transaction history
- `support_ticket` - Help/support tickets
- `support_message` - Ticket message threading

**order_db:**
- `coupon` - Discount coupon definitions
- `coupon_usage` - Tracks who used which coupons
- `return_request` - Return/exchange requests
- `shipment_tracking` - Order tracking events
- `audit_log` - Complete audit trail for orders

**product_db:**
- `category` - Hierarchical product categories
- `product_image` - Multiple images per product
- `product_variant` - Size/color/style variants
- `flash_deal` - Time-limited discount campaigns
- `stock_movement` - Inventory change history

**review_db:**
- `review_image` - Review image attachments
- `review_vote` - Helpful/not helpful votes

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
