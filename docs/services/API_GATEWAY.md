# API Gateway — Documentation

**Public Port:** 8080  
**Framework:** Spring Cloud Gateway (Reactive / WebFlux)  
**Dependencies:** All microservices (downstream)

---

## Table of Contents

- [Overview](#overview)
- [Route Configuration](#route-configuration)
- [JWT Authentication](#jwt-authentication)
- [Public Paths (No Auth Required)](#public-paths-no-auth-required)
- [Header Injection](#header-injection)
- [Global Filters](#global-filters)
- [Rate Limiting](#rate-limiting)
- [CORS Configuration](#cors-configuration)
- [Configuration](#configuration)

---

## Overview

The API Gateway is the **single public entry point** for all client traffic. Every request to the platform goes through `http://localhost:8080`. The gateway:

1. Validates JWT tokens on protected routes
2. Extracts user identity claims and injects them as HTTP headers
3. Routes requests to the appropriate microservice by path prefix
4. Applies rate limiting per IP address
5. Generates correlation IDs for distributed tracing
6. Injects the internal secret for service-to-service authentication

---

## Route Configuration

| Route ID | Path Pattern | Downstream Service | Internal URL |
|----------|-------------|-------------------|--------------|
| auth-service | `/api/auth/**` | Auth Service | `http://auth-service:8080` |
| user-service | `/api/user/**` | User Service | `http://user-service:8080` |
| product-service | `/api/product/**` | Product Service | `http://product-service:8080` |
| order-service | `/api/order/**` | Order Service | `http://order-service:8080` |
| review-service | `/api/review/**` | Review Service | `http://review-service:8080` |
| payment-service | `/api/payment/**` | Payment Service | `http://payment-service:8080` |

---

## JWT Authentication

The gateway validates the `Authorization: Bearer <token>` header on every non-public request.

**Validation steps:**
1. Extract token from `Authorization` header
2. Verify HS256 signature against `jwt.secret`
3. Check token expiry (`exp` claim)
4. Extract claims: `sub` (email), `uuid`, `role`
5. Inject claims as downstream headers (see [Header Injection](#header-injection))
6. If invalid or expired → return `401 Unauthorized`

**JWT Payload Example:**
```json
{
  "sub": "john@example.com",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "role": "BUYER",
  "iat": 1709721600,
  "exp": 1709722500
}
```

---

## Public Paths (No Auth Required)

These paths bypass JWT validation entirely:

| Path | Method | Purpose |
|------|:------:|---------|
| `/api/auth/login` | POST | User login |
| `/api/auth/refresh` | POST | Token refresh |
| `/api/auth/forgot-password` | POST | Password recovery |
| `/api/auth/reset-password` | POST | Password reset |
| `/api/user/register` | POST | User registration |
| `/api/user/verify-otp` | POST | OTP verification |
| `/api/user/resend-otp` | POST | Resend OTP |
| `/api/product/**` | GET | Product browsing (read-only) |
| `/api/category/**` | GET | Category browsing |
| `/api/review/product/**` | GET | Product reviews |
| `/swagger-ui.html` | GET | Swagger UI |
| `/swagger-ui/**` | GET | Swagger resources |
| `/v3/api-docs/**` | GET | OpenAPI docs |
| `/webjars/**` | GET | Static resources |
| `/actuator/**` | GET | Health/metrics |

All other paths require a valid JWT in the `Authorization` header.

---

## Header Injection

After successful JWT validation, the gateway injects these headers into the downstream request:

| Header | Source | Example Value | Purpose |
|--------|--------|---------------|---------|
| `X-User-UUID` | JWT `uuid` claim | `550e8400-e29b-41d4-a716-446655440000` | User identification |
| `X-User-Role` | JWT `role` claim | `BUYER` / `SELLER` / `ADMIN` | Authorization decisions |
| `X-User-Email` | JWT `sub` claim | `john@example.com` | User email |
| `X-Internal-Secret` | Environment variable | `${INTERNAL_SECRET}` | Service-to-service auth |
| `X-Correlation-ID` | Generated or passed through | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` | Distributed tracing |

**Important:** Downstream services **never parse JWTs themselves**. They trust these gateway-injected headers.

---

## Global Filters

### 1. JwtAuthenticationFilter
- **Order:** Runs first on every request
- **Action:** Validates JWT, sets Spring Security context, injects identity headers
- **Skip:** Public paths are excluded

### 2. CorrelationIdGlobalFilter
- **Order:** Runs on every request
- **Action:** If `X-Correlation-ID` header is present, passes it through. Otherwise generates a new UUID.
- **Purpose:** Enables distributed tracing across all services

### 3. RateLimitGlobalFilter
- **Order:** Runs immediately after `CorrelationIdGlobalFilter`
- **Action:** Increments a Redis counter per client IP with a 1-minute TTL
- **Limit:** `100` requests per minute per client IP
- **Response on limit:** `429 Too Many Requests` with `X-Rate-Limit-Retry-After-Seconds`

### 4. InternalSecretFilterConfig
- **Order:** Runs on forwarded requests
- **Action:** Injects `X-Internal-Secret` header into all downstream requests
- **Purpose:** Ensures downstream services can verify the request came through the gateway

---

## Rate Limiting

The gateway uses **Redis-backed distributed counters** for per-IP rate limiting:

- **Key:** Client IP address
- **Algorithm:** Atomic counter + TTL reset window (1 minute)
- **Scope:** Shared across all gateway replicas (no per-node drift)
- **Response when exceeded:**

```http
HTTP/1.1 429 Too Many Requests
X-Rate-Limit-Retry-After-Seconds: 60
```

---

## CORS Configuration

The gateway allows cross-origin requests for frontend integration:

| Setting | Value |
|---------|-------|
| Allowed Origins | `http://localhost:3000`, `http://localhost:3001` |
| Allowed Methods | GET, POST, PUT, DELETE, OPTIONS |
| Allowed Headers | Authorization, Content-Type, X-Correlation-ID |
| Exposed Headers | X-Correlation-ID |

---

## Configuration

```properties
spring.application.name=api-gateway
server.port=8080

# JWT
jwt.secret=${JWT_SECRET}

# Internal Secret
internal.secret=${INTERNAL_SECRET}

# Redis (rate limiting)
spring.data.redis.host=${REDIS_HOST:redis}
spring.data.redis.port=${REDIS_PORT:6379}

# Eureka
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka/

# Routes
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://auth-service:8080
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**

spring.cloud.gateway.routes[1].id=user-service
spring.cloud.gateway.routes[1].uri=http://user-service:8080
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/user/**

spring.cloud.gateway.routes[2].id=product-service
spring.cloud.gateway.routes[2].uri=http://product-service:8080
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/product/**

spring.cloud.gateway.routes[3].id=order-service
spring.cloud.gateway.routes[3].uri=http://order-service:8080
spring.cloud.gateway.routes[3].predicates[0]=Path=/api/order/**

spring.cloud.gateway.routes[4].id=review-service
spring.cloud.gateway.routes[4].uri=http://review-service:8080
spring.cloud.gateway.routes[4].predicates[0]=Path=/api/review/**

spring.cloud.gateway.routes[5].id=payment-service
spring.cloud.gateway.routes[5].uri=http://payment-service:8080
spring.cloud.gateway.routes[5].predicates[0]=Path=/api/payment/**
```

