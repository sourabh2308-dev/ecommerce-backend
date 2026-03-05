# Ecommerce Backend - Comprehensive Code Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture & Design Patterns](#architecture--design-patterns)
3. [File Structure & Organization](#file-structure--organization)
4. [Services Documentation](#services-documentation)
5. [Technical Details](#technical-details)
6. [Database Schema](#database-schema)
7. [API Endpoints](#api-endpoints)
8. [Development Guide](#development-guide)

---

## Project Overview

**Ecommerce Backend** is a microservices-based e-commerce platform with 6 core services, an API Gateway, and service discovery.

### Key Features
- User management with OTP verification
- Product catalog with search/filtering
- Order management with saga pattern
- Payment processing and tracking
- Order reviews and ratings
- Role-based access control (ADMIN, BUYER, SELLER)
- Distributed tracing with Correlation IDs
- Async event processing with Kafka
- Caching with Redis

### Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: PostgreSQL
- **Message Queue**: Kafka
- **Service Discovery**: Eureka
- **API Gateway**: Spring Cloud Gateway
- **Configuration Server**: Spring Cloud Config
- **Caching**: Redis
- **Monitoring**: Prometheus + Grafana
- **Build Tool**: Maven

---

## Architecture & Design Patterns

### 1. Microservices Architecture
```
┌─────────────────────────────────────────────────────┐
│              API Gateway (Port 8080)                │
│  (JWT validation, routing, rate limiting)           │
└──────────────┬──────────────────────────────────────┘
               │
      ┌────────┴────────┐
      │                 │
      ▼                 ▼
  Services:        Config Server
  - User           Eureka Server
  - Auth
  - Product
  - Order
  - Payment
  - Review
```

### 2. Saga Pattern for Orders
Used for distributed transactions across services:

```
Customer              Order Service        Payment Service
   │                      │                      │
   ├─ Create Order ───────→├─ Save Order         │
   │                       ├─ Publish Event ────→┤
   │                       │                     ├─ Process Payment
   │                       │                     ├─ Publish Result
   │                       │←──── Update Status ─┤
   │←── Order Confirmed ───┤
```

### 3. Design Patterns Used

| Pattern | Usage | Example |
|---------|-------|---------|
| **Repository** | Data access abstraction | `OrderRepository.findByBuyerUuid()` |
| **Service Layer** | Business logic isolation | `OrderServiceImpl`, `UserServiceImpl` |
| **DTO** | Cross-layer data transfer | `CreateOrderRequest`, `UserResponse` |
| **Feign Client** | Inter-service HTTP calls | `ProductServiceClient` |
| **Kafka Events** | Async inter-service communication | `OrderCreatedEvent` |
| **Filter** | Request/response intercepting | `JwtAuthenticationFilter` |
| **Configuration** | Externalised config | `SecurityConfig`, `RedisCacheConfig` |
| **Exception Handling** | Custom domain exceptions | `OrderNotFoundException` |
| **Caching** | Performance optimization | `@Cacheable`, `@CacheEvict` |

---

## File Structure & Organization

### Project Layout

```
ecommerce-backend/
├── parent pom.xml (dependency management)
├── docker-compose.yml (services for local development)
├── prometheus.yml (monitoring config)
├── README.md (project overview)
│
├── api-gateway/                    # Spring Cloud Gateway
│   └── src/main/java/.../
│       ├── filter/                 # JWT, correlation ID filters
│       └── config/                 # Gateway routing configuration
│
├── eureka-server/                  # Service discovery
│   └── src/main/java/.../
│       └── EurekaServerApplication.java
│
├── config-server/                  # Centralized configuration
│   └── src/main/java/.../
│       └── ConfigServerApplication.java
│
├── auth-service/                   # Authentication & JWT
│   ├── src/main/java/com/sourabh/auth_service/
│   │   ├── controller/             # JWT token endpoints
│   │   ├── service/                # Token generation/validation
│   │   ├── entity/                 # User & Token entities
│   │   ├── repository/             # Database access
│   │   ├── security/               # Security configuration
│   │   ├── filter/                 # Request interceptors
│   │   ├── exception/              # Auth-specific exceptions
│   │   └── util/                   # Helper functions
│   └── resources/
│       ├── application.properties  # Configuration
│       ├── logback-spring.xml     # Logging config
│       └── db/migration/           # Flyway migrations
│
├── user-service/                   # User management
│   ├── src/main/java/com/sourabh/user_service/
│   │   ├── controller/             # User REST endpoints
│   │   ├── service/                # User business logic
│   │   ├── entity/                 # User, Role entities
│   │   ├── repository/             # User database queries
│   │   ├── dto/                    # Request/Response objects
│   │   ├── config/                 # Caching, Redis config
│   │   ├── exception/              # User-specific exceptions
│   │   └── kafka/                  # Async event handlers
│   └── resources/
│
├── product-service/                # Product catalog
│   ├── src/main/java/com/sourabh/product_service/
│   │   ├── controller/             # Product endpoints
│   │   ├── service/                # Product logic
│   │   ├── entity/                 # Product entity
│   │   ├── repository/             # Product queries
│   │   ├── dto/                    # DTOs
│   │   ├── exception/              # Product exceptions
│   │   └── feign/                  # Calls to other services
│   └── resources/
│
├── order-service/                  # Order management
│   ├── src/main/java/com/sourabh/order_service/
│   │   ├── controller/             # Order endpoints
│   │   ├── service/                # Order saga orchestration
│   │   ├── entity/                 # Order, OrderItem entities
│   │   ├── repository/             # Order queries
│   │   ├── dto/                    # DTOs
│   │   ├── kafka/                  # Event publishing
│   │   ├── feign/                  # Product service calls
│   │   └── exception/              # Order exceptions
│   └── resources/
│
├── payment-service/                # Payment processing
│   ├── src/main/java/com/sourabh/payment_service/
│   │   ├── controller/             # Payment endpoints
│   │   ├── service/                # Payment logic
│   │   ├── entity/                 # Payment entity
│   │   ├── repository/             # Payment queries
│   │   ├── dto/                    # DTOs
│   │   ├── kafka/                  # Event consumption
│   │   └── exception/              # Payment exceptions
│   └── resources/
│
├── review-service/                 # Order reviews
│   ├── src/main/java/com/sourabh/review_service/
│   │   ├── controller/             # Review endpoints
│   │   ├── service/                # Review logic
│   │   ├── entity/                 # Review entity
│   │   ├── repository/             # Review queries
│   │   ├── dto/                    # DTOs
│   │   └── exception/              # Review exceptions
│   └── resources/
│
└── docs/                           # Documentation
    ├── ARCHITECTURE_GUIDE.md       # System design
    ├── API_DOCUMENTATION.md        # Endpoint details
    └── FLOW_AND_CODE_EXPLANATION.md # Data flows
```

---

## Services Documentation

### 1. Auth Service (Port 8081)

**Purpose**: Handles JWT token generation and validation.

**Key Components**:
- `JwtUtil`: Generates tokens and extracts claims
- `AuthController`: Token endpoints (/api/auth/login, /api/auth/validate)
- `AuthServiceImpl`: Core auth logic
- `InternalSecretFilter`: Service-to-service authentication

**Key Features**:
- JWT token generation with claims (uuid, role)
- Token validation and expiration
- OTP-based user onboarding
- Role-based access control

**DTOs**:
- `LoginRequest`: Email + password
- `LoginResponse`: Access token + refresh token
- `ValidateTokenRequest`: Token to validate

### 2. User Service (Port 8082)

**Purpose**: User management, OTP verification, profile updates.

**Key Components**:
- `UserController`: User CRUD endpoints
- `UserServiceImpl`: User operations
- `User` Entity: User domain model
- `UserRepository`: User queries

**Key Features**:
- User registration with OTP
- Email-based authentication
- Profile updates (name, address, etc.)
- Seller details management
- Password change
- Pagination of users (admin only)

**Role-Based Access**:
- **ADMIN**: View/manage all users, create sellers
- **BUYER**: Create orders, write reviews
- **SELLER**: Upload products, manage orders

**DTOs**:
- `RegisterRequest`: New user details
- `VerifyOTPRequest`: OTP verification
- `UpdateProfileRequest`: Profile changes
- `SellerDetailRequest`: Seller information

### 3. Product Service (Port 8083)

**Purpose**: Product catalog management and inventory.

**Key Components**:
- `ProductController`: Product endpoints
- `ProductServiceImpl`: Business logic
- `Product` Entity: Product domain model
- `ProductRepository`: Product queries

**Key Features**:
- Product creation by sellers
- Search/filter by name, price, seller
- Stock management
- Product status (ACTIVE/INACTIVE)
- Pagination

**DTOs**:
- `CreateProductRequest`: New product
- `ProductResponse`: Product details
- `UpdateProductRequest`: Edits

### 4. Order Service (Port 8084)

**Purpose**: Order lifecycle management using saga pattern.

**Key Components**:
- `OrderController`: Order endpoints
- `OrderServiceImpl`: Saga orchestration
- `Order` Entity: Order with items
- `OrderRepository`: Order queries
- Kafka producer for events

**Order Lifecycle**:
```
CREATED → CONFIRMED → SHIPPED → DELIVERED
          ∧                         │
          │                         ▼
       CANCELLED ←─ FAILED ─────────┘
```

**Saga Pattern**:
1. Customer creates order → Order saved (CREATED)
2. Event published → Payment service processes
3. Payment result → Order status updated (SUCCESS/FAILED)

**DTOs**:
- `CreateOrderRequest`: Items + shipping
- `OrderResponse`: Complete order details

### 5. Payment Service (Port 8085)

**Purpose**: Payment processing and tracking.

**Key Components**:
- `PaymentController`: Payment endpoints
- `PaymentServiceImpl`: Payment logic
- `Payment` Entity: Payment records
- Kafka consumer for order events

**Key Features**:
- Listens for order.created events
- Processes payments (currently 100% success mocke)
- Publishes payment.completed events
- Tracks payment status (PENDING/SUCCESS/FAILED)

**Payment Split**:
- Tracks payment distribution to seller accounts

### 6. Review Service (Port 8086)

**Purpose**: Product reviews and ratings.

**Key Components**:
- `ReviewController`: Review endpoints
- `ReviewServiceImpl`: Business logic
- `Review` Entity: Review records
- `ReviewRepository`: Review queries

**Key Features**:
- Create/update/delete reviews
- Rate products (1-5 stars)
- List reviews by product
- Buyer can only modify their own reviews

**DTOs**:
- `CreateReviewRequest`: New review
- `ReviewResponse`: Review details

### 7. API Gateway (Port 8080)

**Purpose**: Single entry point, routing, authentication.

**Key Components**:
- `JwtAuthenticationFilter`: Validates JWT tokens
- `RateLimitGlobalFilter`: Rate limiting
- `CorrelationIdFilter`: Distributed tracing
- `GatewayConfig`: Route definitions

**Responsibilities**:
- Routes requests to appropriate service
- Validates JWT tokens
- Injects headers (X-User-UUID, X-User-Role)
- Rate limiting per user
- CORS handling
- Global error handling

**Routes**:
```
/api/auth/** → Auth Service
/api/user/** → User Service
/api/product/** → Product Service
/api/order/** → Order Service
/api/payment/** → Payment Service
/api/review/** → Review Service
```

---

## Technical Details

### Package Structure (In Each Service)

```
com.sourabh.{service}_service/
├── Controller.java          # REST endpoints
├── service/
│   ├── {Service}.java       # Interface
│   └── impl/
│       └── {Service}Impl.java # Implementation
├── entity/                  # JPA entities
│   └── {Domain}*.java
├── repository/              # Spring Data JPA
│   └── {Domain}Repository.java
├── dto/
│   ├── request/
│   │   └── {Operation}Request.java
│   └── response/
│       └── {Domain}Response.java
├── exception/               # Custom exceptions
│   └── {Domain}*Exception.java
├── kafka/                   # Async messaging
│   ├── event/
│   │   └── {Name}Event.java
│   └── consumer/
│       └── {Name}Consumer.java
├── config/                  # Spring configuration
│   └── {Feature}Config.java
├── filter/                  # HTTP interceptors
└── security/                # Security classes
```

### Annotation Reference

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@RestController` | REST API endpoint class | `@RestController class UserController` |
| `@GetMapping` | HTTP GET endpoint | `@GetMapping("/users")` |
| `@PostMapping` | HTTP POST endpoint | `@PostMapping("/users")` |
| `@Service` | Business logic bean | `@Service class UserServiceImpl` |
| `@Repository` | Data access bean | `public interface UserRepository` |
| `@Entity` | JPA domain model | `@Entity class User` |
| `@Autowired` | Dependency injection | `@Autowired UserRepository repo` |
| `@Transactional` | Transaction management | `@Transactional public void save()` |
| `@Cacheable` | Cache results | `@Cacheable("users", key="#id")` |
| `@CacheEvict` | Invalidate cache | `@CacheEvict("users", key="#id")` |
| `@PreAuthorize` | Role-based security | `@PreAuthorize("hasRole('BUYER')")` |
| `@Valid` | Input validation | `@Valid @RequestBody CreateRequest` |
| `@KafkaListener` | Async message consumption | `@KafkaListener(topics="order.created")` |
| `@FeignClient` | HTTP client generation | `@FeignClient(name="product-service")` |

---

## Database Schema

### Core Entities

#### User
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    phone VARCHAR(20),
    role VARCHAR(20),           -- ADMIN, BUYER, SELLER
    status VARCHAR(20),         -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);
```

#### Product
```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    seller_uuid VARCHAR(36) NOT NULL,
    name VARCHAR(255),
    description TEXT,
    price DECIMAL(10,2),
    stock INTEGER,
    status VARCHAR(20),         -- ACTIVE, INACTIVE
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);
```

#### Order
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    buyer_uuid VARCHAR(36) NOT NULL,
    total_amount DECIMAL(12,2),
    status VARCHAR(20),         -- CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    payment_status VARCHAR(20), -- PENDING, SUCCESS, FAILED
    shipping_name VARCHAR(255),
    shipping_address TEXT,
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(50),
    shipping_pincode VARCHAR(10),
    shipping_phone VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);
```

#### OrderItem
```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_uuid VARCHAR(36) NOT NULL,
    seller_uuid VARCHAR(36) NOT NULL,
    price DECIMAL(10,2),
    quantity INTEGER,
    subtotal DECIMAL(12,2),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

### Indexing Strategy

```sql
-- User Service
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_uuid ON users(uuid);
CREATE INDEX idx_user_role_status ON users(role, status);

-- Product Service
CREATE INDEX idx_product_uuid ON products(uuid);
CREATE INDEX idx_product_seller_uuid ON products(seller_uuid);
CREATE INDEX idx_product_name_status ON products(name, status);

-- Order Service
CREATE INDEX idx_order_uuid ON orders(uuid);
CREATE INDEX idx_order_buyer_uuid ON orders(buyer_uuid);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_created_at ON orders(created_at DESC);
CREATE INDEX idx_order_item_order_id ON order_items(order_id);
CREATE INDEX idx_order_item_seller_uuid ON order_items(seller_uuid);

-- Review Service
CREATE INDEX idx_review_product_uuid ON reviews(product_uuid);
CREATE INDEX idx_review_buyer_uuid ON reviews(buyer_uuid);
```

---

## API Endpoints

### Auth Service
```
POST   /api/auth/login              - Login and get token
POST   /api/auth/validate           - Validate token
POST   /api/auth/refresh            - Refresh expired token
```

### User Service
```
POST   /api/user/register           - Register new user
POST   /api/user/verify-otp         - Verify OTP
POST   /api/user/resend-otp         - Resend OTP
GET    /api/user/profile            - Get logged-in user
PUT    /api/user/profile            - Update profile
PUT    /api/user/password           - Change password
GET    /api/user/internal/{uuid}    - Internal: Get user by UUID
GET    /api/user/all                - Admin: List all users (paginated)
```

### Product Service
```
POST   /api/product                 - Create product (SELLER)
GET    /api/product                 - List products (paginated)
GET    /api/product/{uuid}          - Get product details
PUT    /api/product/{uuid}          - Update product (SELLER)
DELETE /api/product/{uuid}          - Delete product (SELLER)
POST   /api/product/{uuid}/reduce-stock - Reduce stock (internal)
POST   /api/product/{uuid}/restore-stock - Restore stock (internal)
```

### Order Service
```
POST   /api/order                   - Create order (BUYER)
GET    /api/order                   - List user's orders (paginated)
GET    /api/order/{uuid}            - Get order details
GET    /api/order/seller            - List seller's orders (SELLER)
PUT    /api/order/{uuid}/status     - Update order status
PUT    /api/order/internal/payment-update/{uuid} - Update payment (internal)
```

### Payment Service
```
GET    /api/payment/{uuid}          - Get payment details
GET    /api/payment/order/{orderUuid} - Get payment by order
GET    /api/payment/dashboard       - Payment statistics (ADMIN)
```

### Review Service
```
POST   /api/review                  - Create review
GET    /api/review/product/{productUuid} - Get product reviews (paginated)
GET    /api/review/{reviewUuid}     - Get review details
PUT    /api/review/{reviewUuid}     - Update review
DELETE /api/review/{reviewUuid}     - Delete review
GET    /api/review/my-reviews       - Get user's reviews (paginated)
```

---

## Development Guide

### Adding a New Endpoint

1. **Create DTO Classes** (in `dto/request` and `dto/response`)
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    @NotBlank(message = "Name required")
    private String name;
    @Positive(message = "Price must be > 0")
    private BigDecimal price;
}
```

2. **Create Entity** (in `entity`)
```java
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String uuid = UUID.randomUUID().toString();
    
    private String name;
    private BigDecimal price;
}
```

3. **Create Repository** (in `repository`)
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByUuidAndIsDeletedFalse(String uuid);
    Page<Product> findBySellerUuidAndIsDeletedFalse(String sellerUuid, Pageable pageable);
}
```

4. **Create Service Interface** (in `service`)
```java
public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request, String sellerUuid);
    ProductResponse getProductByUuid(String uuid);
}
```

5. **Implement Service** (in `service/impl`)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;
    
    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, String sellerUuid) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .sellerUuid(sellerUuid)
                .build();
        
        Product saved = repository.save(product);
        return mapToResponse(saved);
    }
}
```

6. **Create Controller** (in `controller`)
```java
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService service;
    
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request,
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(service.createProduct(request, sellerUuid));
    }
}
```

### Running Tests
```bash
# Run all tests
mvn test

# Run specific service tests
mvn test -p user-service

# Run with coverage
mvn test jacoco:report
```

### Local Development Setup
```bash
# Start all services with Docker Compose
docker-compose up -d

# Build project
mvn clean package

# Run specific service
mvn spring-boot:run -p user-service

# View logs
docker logs -f <service-name>
```

---

## Code Comments Summary

Every Java file now contains a single-line **purpose comment** indicating:

- **Controllers**: REST API endpoints and request handling
- **Services**: Business logic and data operations
- **Repositories**: Database access layer operations
- **Entities**: Domain model objects persisted in database
- **DTOs**: Request/response data transfer objects
- **Configuration**: Spring beans and infrastructure setup
- **Filters**: Cross-cutting concerns (auth, logging, etc.)
- **Exceptions**: Domain-specific error handling
- **Kafka Events**: Async inter-service messaging
- **Kafka Consumers**: Listening and processing events
- **Feign Clients**: Inter-service HTTP communication

This documentation now provides:
✅ Clear understanding of each file's role
✅ Data flow between layers
✅ Design patterns used
✅ API endpoint reference
✅ Database schema
✅ Development guidelines

For specific implementation details, refer to the inline comments in each Java file.
