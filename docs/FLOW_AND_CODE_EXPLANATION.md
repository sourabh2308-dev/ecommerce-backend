# Flow and Code Explanation (Deep Dive)

This guide explains **what each major part does**, **where to find it in code**, and **how requests/events move across services**.

For full line-by-line source (all included files), use:
- `docs/COMPLETE_CODE_REFERENCE.md`

---

## 1) Runtime Topology

### Entry and control plane
- `api-gateway`: public HTTP entry point and JWT validation boundary.
- `eureka-server`: service registry.
- `config-server`: shared Spring configuration distribution.

### Business services
- `auth-service`: login/refresh/logout, refresh-token persistence.
- `user-service`: registration, OTP verification/resend, profile/admin operations.
- `product-service`: product lifecycle and stock/rating updates.
- `order-service`: order creation/state transitions, calls product-service.
- `payment-service`: payment lifecycle and payment-related queries.
- `review-service`: review CRUD and rating update events.

### Infra services
- PostgreSQL, Redis, Kafka, Zipkin, Prometheus, Grafana.

---

## 2) Bootstrapping and Module Wiring

### Maven multi-module
- Parent: `pom.xml`
- Modules listed for all microservices + eureka + config server.

### Spring Boot entry classes
- `api-gateway/.../ApiGatewayApplication.java`
- `auth-service/.../AuthServiceApplication.java`
- `user-service/.../UserServiceApplication.java`
- `product-service/.../ProductServiceApplication.java`
- `order-service/.../OrderServiceApplication.java`
- `payment-service/.../PaymentServiceApplication.java`
- `review-service/.../ReviewServiceApplication.java`
- `eureka-server/.../EurekaServerApplication.java`
- `config-server/.../ConfigServerApplication.java`

`order-service` and `review-service` enable Feign clients via `@EnableFeignClients`.

---

## 3) HTTP Security Architecture

## 3.1 Gateway security boundary

### Where implemented
- `api-gateway/src/main/java/com/sourabh/api_gateway/security/SecurityConfig.java`
- `api-gateway/src/main/java/com/sourabh/api_gateway/security/JwtAuthenticationManager.java`
- `api-gateway/src/main/java/com/sourabh/api_gateway/security/JwtAuthenticationConverter.java`
- `api-gateway/src/main/java/com/sourabh/api_gateway/security/JwtUtil.java`
- `api-gateway/src/main/java/com/sourabh/api_gateway/security/InternalSecretFilterConfig.java`

### Behavior
1. Public routes are whitelisted (login/refresh/register/otp/swagger/product listing route).
2. All non-public routes require authenticated exchange.
3. JWT is validated in gateway.
4. Gateway strips client-provided `X-User-*` headers (anti-spoofing).
5. Gateway injects trusted headers from JWT claims:
   - `X-User-UUID`
   - `X-User-Role`
   - `X-User-Email`
6. Gateway injects `X-Internal-Secret` for downstream trust checks.

## 3.2 Downstream service trust model

### Where implemented
Each service has:
- `config/InternalSecretFilter.java`
- `config/HeaderRoleAuthenticationFilter.java`
- `config/SecurityConfig.java`

Present in:
- `auth-service`
- `user-service`
- `product-service`
- `order-service`
- `payment-service`
- `review-service`

### Behavior
- `InternalSecretFilter` rejects requests that bypass the gateway/internal chain.
- `HeaderRoleAuthenticationFilter` reconstructs Spring Security auth context from trusted headers.
- `SecurityConfig` applies endpoint access policies using `@PreAuthorize` and request matchers.

---

## 4) Configuration Flow

### Shared config source
- `config-server/src/main/resources/config/application.properties`

Shared concerns configured there:
- Eureka registration settings.
- tracing + zipkin endpoint.
- actuator exposure + prometheus export.
- logging pattern (`traceId`, `spanId`).
- secrets placeholders (`jwt.secret`, `internal.secret`).

### Service-specific local properties
Each service keeps service-local config in:
- `*/src/main/resources/application.properties`

Typical local properties:
- `spring.application.name`
- `server.port`
- datasource details
- kafka topics/bootstrap
- cache settings
- route mappings (gateway)

### Compose-level env source
`docker-compose.yml` uses `x-common-env` anchor to inject shared env values into services.

---

## 5) Request Flow (End to End)

## 5.1 Authenticated business request flow

1. Client sends request to gateway with `Authorization: Bearer ...`.
2. Gateway validates token.
3. Gateway injects user/role/email/internal-secret headers.
4. Request reaches target service through gateway route.
5. Service `InternalSecretFilter` validates internal secret.
6. Service `HeaderRoleAuthenticationFilter` builds auth principal/authorities.
7. Controller method `@PreAuthorize` and service logic execute.

## 5.2 Public request flow

Public route (e.g., login/register or public product fetch) bypasses JWT requirement in gateway policy and proceeds without authenticated context.

---

## 6) Controller-by-Controller Responsibilities

## 6.1 `AuthController`
File: `auth-service/.../controller/AuthController.java`
- `POST /api/auth/login`: authenticate and return token pair.
- `POST /api/auth/refresh`: rotate/renew access context.
- `POST /api/auth/logout`: invalidate refresh token.

## 6.2 `UserController`
File: `user-service/.../controller/UserController.java`
- Public registration/OTP flows.
- Authenticated profile read/update/password change.
- Admin approvals/blocking/search/list/soft-delete/restore.
- Internal lookup endpoints used by other services.

## 6.3 `ProductController`
File: `product-service/.../controller/ProductController.java`
- Seller create/update and seller/admin delete.
- Admin approval/blocking/unblocking.
- Public/role-aware product listing + get-by-uuid.
- Internal stock/rating mutation endpoints.

## 6.4 `OrderController`
File: `order-service/.../controller/OrderController.java`
- Buyer order creation.
- Role-aware list and get by uuid.
- Seller list endpoint.
- Status updates.
- Internal payment status update endpoint.

## 6.5 `PaymentController`
File: `payment-service/.../controller/PaymentController.java`
- Buyer payment initiation.
- Buyer payments listing.
- Payment lookup by payment UUID and order UUID.

## 6.6 `ReviewController`
File: `review-service/.../controller/ReviewController.java`
- Buyer review create/update/delete (or admin delete).
- Public fetch by product/review id.
- Buyer “my reviews” list.

---

## 7) Inter-Service Communication

## 7.1 Synchronous (Feign)
- `order-service/feign/ProductServiceClient.java`
  - fetch product details
  - reduce stock
  - restore stock
- `review-service/feign/OrderServiceClient.java`
  - fetch order details/eligibility context

`FeignConfig` adds internal secret headers so downstream `InternalSecretFilter` accepts requests.

## 7.2 Asynchronous (Kafka)

### Producers/consumers (key)
- `order-service`: produces order-created style events (`OrderServiceImpl`).
- `payment-service`: consumes order-created (`OrderEventConsumer`), produces payment-completed.
- `order-service`: consumes payment-completed (`PaymentEventConsumer`) to update order status.
- `review-service`: produces review-submitted (`ReviewServiceImpl`).
- `product-service`: consumes review-submitted (`ReviewEventConsumer`) to refresh rating data.

---

## 8) Persistence and Data Ownership

- Database-per-service model initialized by `init.sql`.
- Service-level entities/repositories own local schema and data lifecycle.
- No shared DB table ownership across services.

Current note:
- `init.sql` contains duplicate creation of `payment_db`; this should be cleaned to avoid initialization conflicts on fresh volume boot.

---

## 9) Observability Pipeline

- Tracing enabled in shared config; Zipkin endpoint receives spans.
- Actuator + Prometheus metrics exported by each service.
- Prometheus scrapes and Grafana visualizes.
- Correlation IDs are propagated through dedicated filters.

---

## 10) How to Read “Every Line” Efficiently

Because this codebase has many files, use this order:
1. `docs/COMPLETE_CODE_REFERENCE.md` index to jump to file.
2. Read in this sequence:
   - gateway security + route config
   - service security filters/config
   - controllers
   - service impl classes
   - repositories/entities
   - kafka/feign adapters
3. Cross-check with `docker-compose.yml` and Config Server properties for runtime behavior.

---

## 11) Regenerating Full Source Reference

Run:

```bash
./scripts/generate_full_reference.sh
```

This regenerates `docs/COMPLETE_CODE_REFERENCE.md` from current repository state.
