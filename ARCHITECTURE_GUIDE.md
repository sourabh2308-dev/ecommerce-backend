# E-Commerce Backend - Complete Architecture & Development Guide

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Microservices Architecture](#microservices-architecture)
3. [Technology Stack](#technology-stack)
4. [Core Flows](#core-flows)
5. [Service Details](#service-details)
6. [Database Design](#database-design)
7. [Event-Driven Architecture Enhancements](#event-driven-architecture-enhancements)
8. [API Layers & Security](#api-layers--security)
9. [Development Guide](#development-guide)
10. [Testing Strategy](#testing-strategy)

---

## System Overview

### Purpose
This is a **fully scalable, production-ready e-commerce microservices backend** built with:
- **Spring Boot 3.x**: Latest Spring framework for enterprise applications
- **Microservices**: 7 independent services for scalability
- **Event-Driven**: Kafka for asynchronous saga patterns and notifications
- **Distributed**: Service discovery (Eureka), config management (Config Server)
- **Monitoring**: Prometheus metrics + Grafana dashboards
- **SQL Database**: PostgreSQL for transactional consistency
- **Caching**: Redis for performance optimization

### Key Features

✅ User Management (Registration, Auth, OTP, Password Recovery)  
✅ Address Management (Multiple addresses per user)  
✅ Shopping Cart & Wishlist (Persistent cart, wishlist tracking)  
✅ Product Catalog (Categories, Images, Variants, Flash Deals)  
✅ Inventory Tracking (Real-time stock, history logging)  
✅ Shopping Cart & Orders (Multi-seller, Split payments, Coupons)  
✅ Return Management (Return requests, approval workflow, refunds)  
✅ Shipment Tracking (Real-time location updates, delivery notifications)  
✅ Order Invoices (PDF generation, email delivery)  
✅ Payment Processing (Simulated, Saga pattern, Multiple payment splits)  
✅ Review System (Ratings, Comments, Images, Helpful votes)  
✅ Seller Verification (Document submission, Admin approval)  
✅ Loyalty Program (Points earning, redemption on checkout)  
✅ Support Tickets (Customer support, resolution tracking)  
✅ Admin Dashboard (Metrics, Verification queue, Financial analytics)  
✅ Seller Dashboard (Revenue tracking, order analytics)  
✅ Audit Logging (Comprehensive action tracking)  
✅ Distributed Tracing (Correlation IDs, Zipkin)  
✅ Real-time Metrics (Prometheus + Grafana)  

---

## Microservices Architecture

### 🏗️ Service Breakdown

```
┌─────────────────────────────────────────────────────────────┐
│                     API GATEWAY (Spring Cloud Gateway)       │
│              - JWT Token Validation                          │
│              - Request Routing                               │
│              - Rate Limiting                                 │
│              - Correlation ID Injection                      │
└─────────────────┬───────────────────────────────────────────┘
                  │
        ┌─────────┼─────────┬──────────┬────────────┬─────────┬──────────┐
        ▼         ▼         ▼          ▼            ▼         ▼          ▼
   ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
   │  Auth  │ │  User  │ │Product │ │ Order  │ │Payment │ │ Review │ │ Notif. │
   │Service │ │Service │ │Service │ │Service │ │Service │ │Service │ │Service │
   └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘
        │         │         │          │            │         │          │
        └─────────┴─────────┴──────────┴────────────┴─────────┴──────────┘
                  │
        ┌─────────┼──────────────┬──────────────┐
        ▼         ▼              ▼              ▼
    ┌───────┐ ┌────────┐ ┌─────────────┐ ┌──────────┐
    │ Kafka │ │Postgres│ │   Redis     │ │  Eureka  │
    │ (Saga)│ │ (Data) │ │ (Cache)     │ │(Discovery)│
    └───────┘ └────────┘ └─────────────┘ └──────────┘
```

### Service Responsibilities

#### 1. **Auth Service** (Port 8081)
- **Purpose**: User authentication and JWT token management
- **Key Operations**:
  - `POST /api/auth/login` - Validate credentials, issue token pair
  - `POST /api/auth/refresh` - Rotate tokens (old revoked, new issued)
  - `POST /api/auth/logout` - Revoke refresh token
- **Database**: auth_db (RefreshToken storage for revocation)
- **Dependencies**: user-service (fetch user data via internal call)
- **Security**: BCrypt password hashing, HS256 JWT signing
- **Important Classes**:
  - `JwtUtil`: Token generation/validation with JJWT library
  - `AuthServiceImpl`: Business logic for login/refresh/logout
  - `AuthController`: REST endpoints

#### 2. **User Service** (Port 8081)
- **Purpose**: User profile management, seller verification, and account services
- **Key Operations**:
  - `POST /api/user/register` - Create user account
  - `GET /api/user/{uuid}` - Fetch user profile
  - `PUT /api/user/profile` - Update profile
  - `POST /api/user/verify-otp` - Verify email via OTP
  - `POST /api/user/password/recovery-request` - Request password recovery
  - `POST /api/user/password/reset` - Reset password via token
  - `POST /api/user/address` - Add new address
  - `GET /api/user/addresses` - List user addresses
  - `PUT /api/user/address/{uuid}` - Update address
  - `DELETE /api/user/address/{uuid}` - Delete address
  - `GET /api/user/cart` - Get shopping cart items
  - `POST /api/user/cart` - Add to cart
  - `PUT /api/user/cart/{uuid}` - Update cart item quantity
  - `DELETE /api/user/cart/{uuid}` - Remove from cart
  - `GET /api/user/wishlist` - Get wishlist items
  - `POST /api/user/wishlist` - Add to wishlist
  - `DELETE /api/user/wishlist/{uuid}` - Remove from wishlist
  - `GET /api/user/notifications` - Get user notifications
  - `PUT /api/user/notifications/{uuid}/read` - Mark notification as read
  - `GET /api/user/loyalty` - Get loyalty points balance
  - `GET /api/user/loyalty/transactions` - Loyalty points history
  - `POST /api/user/support-tickets` - Create support ticket
  - `GET /api/user/support-tickets` - List user support tickets
  - `PUT /api/user/support-tickets/{uuid}/update` - Add comment to ticket
  - `POST /api/user/seller-details` - Submit seller verification docs
- **Database**: user_db (User, OTPVerification, SellerDetail, Address, CartItem, WishlistItem, Notification, LoyaltyPoint, LoyaltyTransaction, SupportTicket, SupportTicketComment tables)
- **States**: 
  - ACTIVE: Email verified, can place orders
  - PENDING_DETAILS: Seller submitted incomplete info
  - PENDING_APPROVAL: Seller awaiting admin verification
- **Important Classes**:
  - `User`: Main user entity with email, role, status, loyaltyBalance, loyaltyTier
  - `Address`: User's delivery/billing addresses
  - `CartItem`: Shopping cart items with quantity, product ref
  - `WishlistItem`: User's wishlist products
  - `Notification`: User notifications (orders, reviews, loyalty)
  - `LoyaltyPoint`: Loyalty points ledger
  - `LoyaltyTransaction`: Transaction history (earn/redeem)
  - `SupportTicket`: Customer support tickets
  - `SupportTicketComment`: Ticket comments (user + support team)
  - `SellerDetail`: Additional seller info (business name, documents)
  - `OTPVerification`: Email verification tokens
  - `UserServiceImpl`: Handles registration, OTP, profile, addresses, cart, wishlist, loyalty, support

#### 3. **Product Service** (Port 8081)
- **Purpose**: Product catalog management and inventory tracking
- **Key Operations**:
  - `POST /api/product` - Create product (seller)
  - `GET /api/product/{uuid}` - Fetch product details
  - `GET /api/product` - Search/paginate products
  - `PUT /api/product/{uuid}` - Update product (seller)
  - `DELETE /api/product/{uuid}` - Soft-delete product
  - `GET /api/category` - List product categories
  - `POST /api/product/{uuid}/images` - Add product images
  - `DELETE /api/product/image/{uuid}` - Delete product image
  - `POST /api/product/{uuid}/variants` - Create product variants
  - `GET /api/product/{uuid}/variants` - List product variants
  - `PUT /api/product/variant/{uuid}` - Update variant
  - `POST /api/flash-deals` - Create flash deal (admin)
  - `GET /api/flash-deals` - Get active flash deals
  - `GET /api/product/{uuid}/inventory` - Get inventory details
  - `GET /api/product/{uuid}/inventory-history` - Inventory change history
- **Database**: product_db (Product, Category, ProductImage, ProductVariant, FlashDeal, InventoryHistory tables)
- **Caching**: Redis (product details, search results, flash deals)
- **Features**:
  - Product categories hierarchy
  - Multiple images per product (with alt text)
  - Product variants (size, color, etc.)
  - Flash deals with limited time windows
  - Real-time inventory tracking with history
  - Stock management (created by order-service via Feign)
- **Important Classes**:
  - `Product`: Entity with price, stock, seller, status, avgRating, reviewCount
  - `Category`: Product categories with hierarchy
  - `ProductImage`: Product images with URL and alt text
  - `ProductVariant`: Product variants (size, color, SKU)
  - `FlashDeal`: Limited-time deals with discount
  - `InventoryHistory`: Inventory change tracking for audits
  - `ProductServiceImpl`: CRUD, search, stock, variants, flash deals
  - `ProductController`: API endpoints

#### 4. **Order Service** (Port 8081)
- **Purpose**: Order management, fulfillment, returns, and shipment tracking
- **Key Operations**:
  - `POST /api/order` - Create order (buyer)
  - `GET /api/order/{uuid}` - Fetch order details
  - `PUT /api/order/{uuid}/status` - Update status (admin)
  - `GET /api/order` - List buyer's orders (paginated)
  - `POST /api/order/{uuid}/apply-coupon` - Apply discount coupon
  - `GET /api/order/available-coupons` - List available coupons
  - `POST /api/order/{uuid}/return-request` - Request return
  - `GET /api/order/{uuid}/return-status` - Get return request status
  - `PUT /api/order/{uuid}/return-request/approve` - Approve return (admin)
  - `PUT /api/order/{uuid}/return-request/reject` - Reject return (admin)
  - `GET /api/order/{uuid}/shipment-tracking` - Get delivery location
  - `POST /api/order/{uuid}/shipment-event` - Log shipment event
  - `GET /api/order/{uuid}/invoice` - Get order invoice PDF
  - `GET /api/order/{uuid}/invoice/email` - Email invoice to buyer
  - `GET /api/order/dashboard` - Buyer's order analytics
  - `GET /api/admin/dashboard/orders` - Admin order analytics
- **Database**: order_db (Order, OrderItem, Coupon, CouponUsage, ReturnRequest, ShipmentTracking, OrderInvoice, AuditLog tables)
- **Event Flow**:
  - Creates order → publishes `order.created` Kafka event
  - Consumes `payment.completed` → updates paymentStatus
  - Consumes `order.return.status.changed` → updates return status
  - Saga pattern: Payment failure → cancels order, restores stock
- **Order Status**: CREATED → CONFIRMED → SHIPPED → DELIVERED → (optional RETURNED)
- **Payment Status**: PENDING → SUCCESS/FAILED
- **Return Status**: PENDING → APPROVED/REJECTED → (optional REFUNDED)
- **Shipment Status**: CREATED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
- **Important Classes**:
  - `Order`: Main order entity with items, total, status, discounts, loyalty tracking
  - `OrderItem`: Individual item in order (qty, price, seller)
  - `Coupon`: Discount coupons with validity, discount rules
  - `CouponUsage`: Tracks coupon redemptions per user
  - `ReturnRequest`: Return request with reason and status
  - `ShipmentTracking`: Shipment events with location and timestamp
  - `OrderInvoice`: Invoice data and PDF generation
  - `AuditLog`: Comprehensive action audit trail
  - `PaymentEventConsumer`: Listens to payment events, updates order
  - `OrderServiceImpl`: CRUD, status transitions, payment saga, returns, coupons, shipments
  - `ReturnRequestProcessor`: Handles return approval/rejection workflow

#### 5. **Payment Service** (Port 8081)
- **Purpose**: Payment processing and settlement
- **Key Operations**:
  - `POST /api/payment` - Process payment (buyer)
  - `GET /api/payment` - Fetch payment history (buyer)
  - `GET /api/seller/dashboard` - Seller payment dashboard
  - `GET /api/admin/dashboard` - Admin finance dashboard
  - `POST /api/payment/{uuid}/refund` - Initiate refund
- **Database**: payment_db (Payment, PaymentSplit tables)
- **Event Flow**:
  - Consumes `order.created` Kafka event
  - Simulates payment (100% success for demo)
  - Publishes `payment.completed` event → triggers order status update
- **Features**:
  - Split payments (multi-seller orders)
  - Platform commission calculation (10% default)
  - Delivery fees per item (₹30 default)
- **Important Classes**:
  - `Payment`: Main payment entity with amount, status, order UUID
  - `PaymentSplit`: Settlement record per seller
  - `OrderEventConsumer`: Listens for order.created, initiates payment
  - `PaymentServiceImpl`: Payment processing, splits, dashboards

#### 6. **Review Service** (Port 8081)
- **Purpose**: Product reviews, ratings, images, and community feedback
- **Key Operations**:
  - `POST /api/review` - Create review (buyer, post-delivery)
  - `GET /api/review/product/{uuid}` - Get product reviews (with images)
  - `PUT /api/review/{uuid}` - Update own review
  - `DELETE /api/review/{uuid}` - Delete review
  - `GET /api/review/my-reviews` - Get my reviews
  - `POST /api/review/{uuid}/images` - Upload review images
  - `DELETE /api/review/image/{uuid}` - Delete review image
  - `POST /api/review/{uuid}/vote` - Vote on review (helpful/unhelpful)
  - `DELETE /api/review/{uuid}/vote` - Remove vote
  - `GET /api/review/product/{uuid}/stats` - Get review statistics
- **Database**: review_db (Review, ReviewImage, ReviewVote tables)
- **Caching**: Redis (product review aggregate, review stats)
- **Constraints**:
  - Only buyers who've purchased product can review
  - Only after order is DELIVERED
  - One review per buyer per product
  - Images must be product-related
- **Important Classes**:
  - `Review`: Entity with rating (1-5), comment, timestamp, helpfulness score
  - `ReviewImage`: Review images with secure URLs
  - `ReviewVote`: Community voting on review helpfulness
  - `ReviewServiceImpl`: CRUD, duplicate check, delivery validation, voting
  - `ReviewController`: API endpoints

#### 7. **Notification Service** (Port 8081) - NEW 2026
- **Purpose**: Send notifications across multiple channels
- **Key Operations**:
  - `GET /api/notification/preferences/{uuid}` - Get notification preferences
  - `PUT /api/notification/preferences/{uuid}` - Update preferences
  - `GET /api/notification/history` - Get notification history
- **Event Consumers**:
  - Consumes `notification.created` event
  - Routes to email, SMS, or in-app notification
- **Database**: notification_db (Notification, NotificationPreference tables)
- **Channels**: In-app, Email, SMS, Push notifications

---

## Technology Stack

### Core Framework
- **Spring Boot 3.3**: Latest version with native compilation support
- **Spring Cloud**: Service discovery, config management, gateway

### Data
- **PostgreSQL**: Transactional database (one DB per service)
- **Spring Data JPA**: ORM with Hibernate
- **Flyway**: Database version control and migrations
- **Redis**: Distributed caching (session, product data)

### Messaging
- **Apache Kafka**: Event streaming for sagas and notifications
- **Spring Kafka**: Kafka integration with Spring

### Security
- **Spring Security**: OAuth2-ready, method-level authorization
- **JJWT**: JWT token generation and validation
- **BCrypt**: Password hashing algorithm

### API & Documentation
- **SpringdOC v2**: OpenAPI 3.0 documentation (Swagger)
- **Feign**: Declarative REST client (inter-service calls)
- **Resilience4j**: Circuit breaker, retry, timeout patterns

### Monitoring & Tracing
- **Micrometer**: Metrics collection framework
- **Prometheus**: Metrics scraping and storage
- **Grafana**: Metrics visualization and dashboards
- **Zipkin**: Distributed tracing (correlation IDs)
- **SLF4J + Logback**: Unified logging framework

### Development
- **Lombok**: Boilerplate reduction (@Getter, @Builder, @Slf4j)
- **JUnit 5**: Testing framework
- **Mockito**: Mocking for unit tests
- **Testcontainers**: Docker container test support
- **AssertJ**: Fluent assertion library

---

## Core Flows

### 🔐 User Registration and Login Flow

```
CLIENT (Frontend)
  |
  ├─→ POST /api/user/register {email, password, role}
  │   │
  │   └→ USER-SERVICE
  │       ├─ Validate email format
  │       ├─ Hash password with BCrypt
  │       ├─ Check email not already registered
  │       ├─ Create User entity (status=PENDING_EMAIL)
  │       ├─ Generate OTP
  │       ├─ Send email with OTP
  │       └─ Return {userId, email, status}
  │
  ├─→ POST /api/user/verify-otp {email, otp}
  │   │
  │   └→ USER-SERVICE
  │       ├─ Validate OTP not expired (5 min)
  │       ├─ Mark email as verified
  │       ├─ Update user status to ACTIVE
  │       └─ Return {verified: true}
  │
  └─→ POST /api/auth/login {email, password}
      │
      └→ AUTH-SERVICE
          ├─ Call user-service via RestTemplate
          │  (GET /api/user/internal/email/{email})
          ├─ Fetch user password hash
          ├─ Validate password with BCrypt.matches()
          ├─ Check emailVerified && accountActive
          ├─ Generate JWT access token (15 min expiry)
          │  Payload: {sub: email, uuid: userUuid, role: role, iat, exp}
          ├─ Generate refresh token (UUID, 7 day expiry)
          ├─ Store refresh token in auth_db
          └─ Return {accessToken, refreshToken, tokenType: "Bearer"}

API Gateway (for subsequent requests):
  ├─ Extract JWT from Authorization: Bearer header
  ├─ Validate signature with secret key
  ├─ Check expiry (fail if exp < now)
  ├─ Extract claims (uuid, role)
  ├─ Forward request with X-User-Uuid, X-User-Role headers
  └─ If JWT expired: client calls POST /api/auth/refresh {refreshToken}
      └─ Auth-Service rotates tokens (revoke old, issue new pair)
```

### 🛒 Order Creation Flow (Synchronous + Asynchronous Saga)

```
CLIENT (Buyer)
  |
  └─→ POST /api/order {items: [{productUuid, quantity}, ...]}
       │
       ├─→ API-GATEWAY (validate JWT, extract X-User-Uuid)
       │
       └─→ ORDER-SERVICE
           ├─ Validate role == "BUYER"
           ├─ For each item:
           │  ├─ Call product-service (Feign): getProduct(productUuid)
           │  ├─ Check if product ACTIVE
           │  ├─ Check if stock >= quantity
           │  ├─ Call product-service: reduceStock(productUuid, qty)
           │  └─ Create OrderItem entity
           │
           ├─ Calculate totalAmount = sum(price * qty for all items)
           ├─ Create Order entity
           │  {uuid, buyerUuid, items, totalAmount, status: CREATED, paymentStatus: PENDING}
           ├─ Save Order to order_db
           │
           ├─ PUBLISH KAFKA EVENT: order.created
           │  Topic: order.created
           │  Payload: {orderUuid, buyerUuid, amount, items: [{productUuid, sellerUuid, amount}, ...]}
           │  Partitions: Ensures order events for same buyer go to same partition (ordered)
           │
           └─ RESPOND TO CLIENT: OrderResponse {uuid, items, totalAmount, paymentStatus: "PENDING"}

       ▼ ASYNCHRONOUS (Kafka Saga)

       PAYMENT-SERVICE (consumes order.created)
           ├─ Listen on order.created topic (group: payment-service)
           ├─ Check if event already processed (idempotency via ProcessedEvent table)
           ├─ Create Payment entity {uuid, orderUuid, amount, status: INITIATED}
           ├─ Simulate payment gateway (100% success for demo)
           ├─ Update Payment {status: SUCCESS} (or FAILED randomly)
           ├─ Create PaymentSplit entries per seller
           │  ├─ Seller amount = (orderAmount - fees) * seller_ratio
           │  ├─ Platform commission = amount * 10%
           │  ├─ Delivery fees = qty * ₹30
           │
           ├─ PUBLISH KAFKA EVENT: payment.completed
           │  Topic: payment.completed
           │  Payload: {orderUuid, paymentUuid, status: SUCCESS}
           │  Partition: Same as order to maintain causality
           │
           └─ Mark ProcessedEvent (prevent duplicate processing)

       ORDER-SERVICE (consumes payment.completed)
           ├─ Listen on payment.completed topic (group: order-service)
           ├─ Fetch Order by orderUuid
           ├─ Update Order {paymentStatus: SUCCESS}
           ├─ If status == FAILED:
           │  ├─ Update Order {status: CANCELLED, paymentStatus: FAILED}
           │  ├─ Call product-service: restoreStock(productUuid, qty) for each item
           │  └─ Saga compensation (rollback)
           │
           └─ Save updated Order

       CLIENT (Frontend)
           ├─ Polling: GET /api/order/{orderUuid}
           │  Every 2 seconds while paymentStatus == "PENDING"
           │  Stops when status changes to SUCCESS/FAILED
           │
           └─ On SUCCESS: Display confirmation, redirect to order details
```

### 💰 Payment Processing (Multi-Seller Split)

```
Assume Order with 2 sellers:
  - Seller A: 1 item @ ₹1000
  - Seller B: 2 items @ ₹500 each = ₹1000
  - Total: ₹2000

Payment Calculation:
  1. Total Amount: ₹2000
  2. Platform Commission (10%): ₹200
  3. Delivery Fees (per item, ₹30 each, 3 items): ₹90
  4. Seller A receives: ₹1000 - (₹200 * 1000/2000) - (₹30 * 1 item) = ₹900
  5. Seller B receives: ₹1000 - (₹200 * 1000/2000) - (₹30 * 2 items) = ₹840
  6. Platform keeps: ₹200 + ₹90 - refunds = ₹290

PaymentSplit entities created:
  - id: 1, paymentUuid, sellerUuid: A, amount: ₹900, status: PENDING
  - id: 2, paymentUuid, sellerUuid: B, amount: ₹840, status: PENDING
  - Commission entry, status: SETTLED

Seller Dashboard shows:
  - Available balance: Sum of PAID PaymentSplit amounts
  - Pending balance: Sum of PENDING PaymentSplit amounts
```

### 🛒 Shopping Cart Flow (NEW 2026)

```
CLIENT (Buyer)
  |
  ├─→ GET /api/user/cart
  │   │
  │   └─→ USER-SERVICE
  │       ├─ Fetch cart items for userData
  │       ├─ For each item, fetch product details (with current price)
  │       ├─ Calculate subtotal per item
  │       └─ Return {items: [{productUuid, qty, price, subtotal}, ...]}
  │
  ├─→ POST /api/user/cart {productUuid, quantity}
  │   │
  │   └─→ USER-SERVICE
  │       ├─ Call product-service: getProduct(productUuid)
  │       ├─ Validate product.status == "ACTIVE"
  │       ├─ Check if item already in cart
  │       │  ├─ If yes: Update quantity
  │       │  └─ If no: Create new CartItem
  │       ├─ Save CartItem {uuid, userUuid, productUuid, quantity}
  │       └─ Return updated cart
  │
  ├─→ PUT /api/user/cart/{itemUuid} {quantity}
  │   │
  │   └─→ USER-SERVICE
  │       ├─ Validate quantity > 0
  │       ├─ Check product stock >= quantity
  │       ├─ Update CartItem quantity
  │       └─ Return updated cart
  │
  ├─→ DELETE /api/user/cart/{itemUuid}
  │   │
  │   └─→ USER-SERVICE
  │       ├─ Remove CartItem from cart
  │       └─ Return updated cart
  │
  └─→ POST /api/order (cart → order)
      │
      └─→ ORDER-SERVICE
          ├─ Call user-service: getCart(userUuid)
          ├─ For each cart item:
          │  ├─ Validate product stock
          │  ├─ Create OrderItem
          │  └─ Call product-service: reduceStock(productUuid, qty)
          ├─ Create Order with all items
          ├─ Clear cart (user-service call or event)
          └─ Continue with order saga (payment, etc.)
```

### ⭐ Review Creation Flow

```
CLIENT (Buyer, post-delivery)
  |
  └─→ POST /api/review {orderUuid, productUuid, rating, comment}
       │
       └─→ REVIEW-SERVICE
           ├─ Validate role == "BUYER"
           ├─ Call order-service (Feign): getOrder(orderUuid)
           ├─ Check order.buyerUuid == requester UUID
           ├─ Check order.status == "DELIVERED"
           ├─ Check productUuid in order.items
           ├─ Check no existing review by same buyer for same product
           │  (idempotency: allow update, disallow duplicate)
           │
           ├─ Create Review entity
           │  {uuid, orderUuid, productUuid, sellerUuid, buyerUuid, rating, comment}
           │
           ├─ PUBLISH KAFKA EVENT: review.submitted
           │  Topic: review.submitted
           │  Payload: {reviewUuid, productUuid, rating, buyerUuid}
           │
           ├─ Invalidate product review cache (Redis)
           │
           └─ RESPOND: ReviewResponse {uuid, productUuid, rating, comment, createdAt}

       PRODUCT-SERVICE (consumes review.submitted)
           ├─ Update product review aggregate (cached)
           │  ├─ Increment review count
           │  ├─ Recalculate average rating
           │  └─ Invalidate cache
           │
           └─ No direct response needed (async)
```

### ↩️ Return Request Flow (NEW 2026)

```
CLIENT (Buyer)
  |
  └─→ POST /api/order/{orderUuid}/return-request {reason, comments}
       │
       └─→ ORDER-SERVICE
           ├─ Validate order.buyerUuid == requester UUID
           ├─ Check order.status == "DELIVERED"
           ├─ Check order within 30-day return window
           ├─ Check no existing pending return
           │
           ├─ Create ReturnRequest entity
           │  {uuid, orderUuid, reason, status: PENDING, createdAt}
           │
           ├─ PUBLISH KAFKA EVENT: order.return.requested
           │  Payload: {returnRequestUuid, orderUuid, reason}
           │
           ├─ PUBLISH NOTIFICATION: Send to admin
           │
           └─ RESPOND: ReturnRequestResponse {uuid, status: PENDING}

   ADMIN WORKFLOW
        │
        ├─→ PUT /api/order/{orderUuid}/return-request/approve {approvalNotes}
        │   │
        │   └─→ ORDER-SERVICE (admin role)
        │       ├─ Update ReturnRequest {status: APPROVED, reviewedAt}
        │       ├─ Update Order {status: RETURNED}
        │       ├─ Call payment-service: createRefund(orderUuid, amount)
        │       │  └─ Initiates refund to buyer's original payment method
        │       ├─ PUBLISH KAFKA EVENT: order.return.status.changed
        │       │  Payload: {returnRequestUuid, status: APPROVED}
        │       └─ PUBLISH NOTIFICATION: Send approval to buyer
        │
        └─→ PUT /api/order/{orderUuid}/return-request/reject {rejectionReason}
            │
            └─→ ORDER-SERVICE (admin role)
                ├─ Update ReturnRequest {status: REJECTED, rejectionReason}
                ├─ PUBLISH KAFKA EVENT: order.return.status.changed
                │  Payload: {returnRequestUuid, status: REJECTED}
                └─ PUBLISH NOTIFICATION: Send rejection to buyer

   COMPENSATION (if refund fails)
        └─ ORDER-SERVICE
           ├─ Check refund payment status
           ├─ If failed: Update ReturnRequest {status: REFUND_FAILED}
           ├─ PUBLISH NOTIFICATION: Alert admin and buyer
           └─ Manual intervention required
```

### 📍 Shipment Tracking Flow (NEW 2026)

```
CLIENT (Buyer)
  |
  └─→ GET /api/order/{orderUuid}/shipment-tracking
       │
       └─→ ORDER-SERVICE
           ├─ Fetch all ShipmentTracking events for order
           ├─ Sort by timestamp DESC (latest first)
           ├─ For each event:
           │  └─ Return {location, timestamp, status, description}
           │     Example: "In transit - Mumbai, Maharashtra", "2026-02-25 14:30"
           ├─ Determine current status from latest event
           │  (CREATED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED)
           └─ RESPOND: {currentStatus, currentLocation, eventHistory: [...]}

   LOGISTICS INTEGRATION (periodic, can be webhook)
        │
        └─→ POST /api/order/{orderUuid}/shipment-event
            (Logistics provider pushes updates)
            Payload: {orderId, currentLocation, status, timestamp}
            │
            └─→ ORDER-SERVICE
                ├─ Validate order exists
                ├─ Create ShipmentTracking entity
                │  {uuid, orderUuid, location, status, timestamp}
                ├─ Update Order {shippingStatus: status}
                │
                ├─ If status == "DELIVERED":
                │  ├─ Update Order {status: DELIVERED}
                │  ├─ PUBLISH KAFKA EVENT: order.delivered
                │  ├─ PUBLISH NOTIFICATION: Delivery confirmation to buyer
                │  └─ Enable review submission for buyer
                │
                └─ RESPOND: {event: created, status: SAVED}

   CLIENT NOTIFICATIONS
        └─ USER-SERVICE (consumes order.delivered)
            ├─ Read notification preferences
            ├─ Send email/SMS/push notification
            └─ Create in-app notification
```

### 🎟️ Coupon Validation Flow (NEW 2026)

```
CLIENT (Buyer)
  |
  └─→ POST /api/order/{orderUuid}/apply-coupon {couponCode}
       │
       └─→ ORDER-SERVICE
           ├─ Fetch coupon by code
           ├─ Validate coupon:
           │  ├─ Check coupon.status == "ACTIVE"
           │  ├─ Check current time within coupon.validFrom and validTo
           │  ├─ Check order.totalAmount >= coupon.minOrderAmount
           │  ├─ Check buyer hasn't exceeded coupon.maxUsagePerUser
           │  ├─ Check coupon.usageCount < coupon.maxTotalUsage
           │  └─ Check coupon not already applied to this order
           │
           ├─ Calculate discount:
           │  ├─ If coupon.type == "PERCENTAGE":
           │  │  └─ discountAmount = order.amount * (coupon.discountValue / 100)
           │  └─ If coupon.type == "FIXED":
           │     └─ discountAmount = coupon.discountValue
           │
           ├─ Apply caps:
           │  ├─ discountAmount = min(discountAmount, coupon.maxDiscountAmount)
           │  └─ finalAmount = order.amount - discountAmount
           │
           ├─ Create CouponUsage entity
           │  {uuid, orderUuid, buyerUuid, couponCode, discountAmount}
           │
           ├─ Update Order
           │  {couponCode, discount: discountAmount, finalAmount}
           │
           ├─ Increment coupon usage counters
           │  (database: coupon.usageCount++, buyerUsage incrementBy 1)
           │
           └─ RESPOND: OrderResponse {totalAmount, discountAmount, finalAmount}

   VALIDATION FAILURE CASES
        ├─→ Expired coupon
        │   └─ Return: {status: FAILED, reason: "Coupon expired"}
        ├─→ Insufficient order value
        │   └─ Return: {status: FAILED, reason: "Order must be at least ₹{minAmount}"}
        ├─→ Already used max times
        │   └─ Return: {status: FAILED, reason: "You've used this coupon {maxUses} times"}
        └─→ Coupon limit reached
            └─ Return: {status: FAILED, reason: "Coupon usage limit exceeded"}
```

### ⭐ Loyalty Points Flow (NEW 2026)

```
AUTOMATIC EARNING (on successful payment)
  └─→ PAYMENT-SERVICE (consumes order.created)
       ├─ Calculate points earned
       │  └─ points = order.amount * loyalty.pointsPerRupee
       │     (default: 1 point per ₹1)
       ├─ Create LoyaltyPoint entry (not yet credited)
       └─ PUBLISH KAFKA EVENT: loyalty.points.earned
          Payload: {buyerUuid, orderUuid, points, timestamp}

    USER-SERVICE (consumes loyalty.points.earned)
      ├─ Fetch current loyalty balance for buyer
      ├─ Create LoyaltyTransaction {type: EARNED, points, orderUuid}
      ├─ Update User {loyaltyBalance: balance + points}
      ├─ Create Notification: "You earned {points} loyalty points!"
      └─ Invalidate loyalty cache

REDEMPTION (at checkout)
  └─→ CLIENT: POST /api/order {items: [...], redeemLoyaltyPoints: 500}
       │
       └─→ ORDER-SERVICE
           ├─ Call user-service (Feign): getLoyaltyBalance(buyerUuid)
           ├─ Validate Balance >= redeemLoyaltyPoints
           ├─ Get redemption rate (default: 1 point = ₹0.5)
           ├─ Calculate discount = redeemLoyaltyPoints * redemptionRate
           ├─ Validate discount <= order.amount (can't exceed order total)
           │
           ├─ Create LoyaltyTransaction (via user-service):
           │  {type: REDEEMED, points: redeemLoyaltyPoints, orderUuid}
           │
           ├─ Update Order {loyaltyPointsRedeemed, loyaltyDiscount}
           │
           └─ Apply to final amount:
              finalAmount = order.amount - loyaltyDiscount - (coupon if any)

POINT EXPIRY & TIER BONUSES (background job)
  └─→ Scheduled Task (daily)
       ├─ Find LoyaltyTransactions with type=EARNED older than 1 year
       ├─ Update to EXPIRED
       ├─ Deduct from user loyalty balance
       ├─ Log expiry events
       └─ Send notification: "Your {points} loyalty points expired"

TIER BENEFITS
  └─→ USER profiles have loyalty tier (BRONZE, SILVER, GOLD)
       ├─ BRONZE (0-999 points): 1 point per ₹1 spent
       ├─ SILVER (1000-4999): 1.2 points per ₹1 spent
       └─ GOLD (5000+): 1.5 points per ₹1 spent
          └─ Also get: 5% birthday discount, free shipping on ₹500+ orders
```

---

## Service Details

### API Endpoint Reference

| Service | Method | Endpoint | Description |
|---------|--------|----------|-------------|
| **Auth** | POST | /api/auth/login | Login with email/password |
| | POST | /api/auth/refresh | Rotate JWT tokens |
| | POST | /api/auth/logout | Revoke refresh token |
| **User** | POST | /api/user/register | Create new user |
| | GET | /api/user/{uuid} | Get user profile |
| | PUT | /api/user/profile | Update user profile |
| | POST | /api/user/verify-otp | Verify email OTP |
| | POST | /api/user/password/recovery-request | Request password reset |
| | POST | /api/user/password/reset | Reset password with token |
| | POST | /api/user/address | Add new address |
| | GET | /api/user/addresses | List user addresses |
| | PUT | /api/user/address/{uuid} | Update address |
| | DELETE | /api/user/address/{uuid} | Delete address |
| | GET | /api/user/cart | Get shopping cart |
| | POST | /api/user/cart | Add to cart |
| | PUT | /api/user/cart/{uuid} | Update cart quantity |
| | DELETE | /api/user/cart/{uuid} | Remove from cart |
| | GET | /api/user/wishlist | Get wishlist items |
| | POST | /api/user/wishlist | Add to wishlist |
| | DELETE | /api/user/wishlist/{uuid} | Remove from wishlist |
| | GET | /api/user/notifications | Get user notifications |
| | PUT | /api/user/notifications/{uuid}/read | Mark as read |
| | GET | /api/user/loyalty | Get loyalty balance |
| | GET | /api/user/loyalty/transactions | Loyalty history |
| | POST | /api/user/support-tickets | Create support ticket |
| | GET | /api/user/support-tickets | List support tickets |
| | PUT | /api/user/support-tickets/{uuid}/update | Add ticket comment |
| | POST | /api/user/seller-details | Submit seller verification |
| **Product** | POST | /api/product | Create product (seller) |
| | GET | /api/product/{uuid} | Get product details |
| | GET | /api/product | Search/list products |
| | PUT | /api/product/{uuid} | Update product |
| | DELETE | /api/product/{uuid} | Soft-delete product |
| | GET | /api/category | List categories |
| | POST | /api/product/{uuid}/images | Add product images |
| | DELETE | /api/product/image/{uuid} | Delete product image |
| | POST | /api/product/{uuid}/variants | Create variant |
| | GET | /api/product/{uuid}/variants | List variants |
| | PUT | /api/product/variant/{uuid} | Update variant |
| | POST | /api/flash-deals | Create flash deal (admin) |
| | GET | /api/flash-deals | Get active flash deals |
| | GET | /api/product/{uuid}/inventory | Get inventory details |
| | GET | /api/product/{uuid}/inventory-history | Inventory history |
| **Order** | POST | /api/order | Create order (buyer) |
| | GET | /api/order/{uuid} | Get order details |
| | GET | /api/order | List my orders |
| | PUT | /api/order/{uuid}/status | Update status (admin) |
| | POST | /api/order/{uuid}/apply-coupon | Apply discount coupon |
| | GET | /api/order/available-coupons | List available coupons |
| | POST | /api/order/{uuid}/return-request | Request return |
| | GET | /api/order/{uuid}/return-status | Get return status |
| | PUT | /api/order/{uuid}/return-request/approve | Approve return (admin) |
| | PUT | /api/order/{uuid}/return-request/reject | Reject return (admin) |
| | GET | /api/order/{uuid}/shipment-tracking | Get delivery location |
| | POST | /api/order/{uuid}/shipment-event | Log shipment event |
| | GET | /api/order/{uuid}/invoice | Get invoice PDF |
| | GET | /api/order/{uuid}/invoice/email | Email invoice |
| | GET | /api/order/dashboard | Buyer order analytics |
| | GET | /api/admin/dashboard/orders | Admin order analytics |
| **Payment** | POST | /api/payment | Initiate payment |
| | GET | /api/payment | Payment history |
| | GET | /api/seller/dashboard | Seller payment dashboard |
| | GET | /api/admin/dashboard | Admin finance dashboard |
| | POST | /api/payment/{uuid}/refund | Initiate refund |
| **Review** | POST | /api/review | Create review |
| | GET | /api/review/product/{uuid} | Get product reviews |
| | PUT | /api/review/{uuid} | Update review |
| | DELETE | /api/review/{uuid} | Delete review |
| | GET | /api/review/my-reviews | Get my reviews |
| | POST | /api/review/{uuid}/images | Upload review images |
| | DELETE | /api/review/image/{uuid} | Delete review image |
| | POST | /api/review/{uuid}/vote | Vote on review |
| | DELETE | /api/review/{uuid}/vote | Remove vote |
| | GET | /api/review/product/{uuid}/stats | Review statistics |

---

## Database Design

### Core Entities and Relationships

```
USER-DB:
  ┌──────────────────────┐
  │    User              │
  ├──────────────────────┤
  │ uuid (PK)            │
  │ email                │ (unique, indexed)
  │ password             │ (hashed BCrypt)
  │ firstName            │
  │ lastName             │
  │ role                 │ (BUYER, SELLER, ADMIN)
  │ status               │ (ACTIVE, PENDING_DETAILS, PENDING_APPROVAL)
  │ emailVerified        │ (boolean)
  │ phone                │
  │ loyaltyBalance       │ (integer, default 0)
  │ loyaltyTier          │ (BRONZE, SILVER, GOLD)
  └──────────────────────┘
        │
        ├─→ SellerDetail (one-to-one for sellers)
        │   ├─ businessName
        │   ├─ gstNumber
        │   ├─ bankAccounts
        │   └─ status (APPROVED, PENDING, REJECTED)
        │
        ├─→ Address (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ userUuid (FK)
        │   ├─ street
        │   ├─ city
        │   ├─ state
        │   ├─ pincode
        │   ├─ isDefault
        │   └─ type (HOME, WORK, OTHER)
        │
        ├─→ CartItem (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ userUuid (FK)
        │   ├─ productUuid
        │   ├─ quantity
        │   └─ addedAt
        │
        ├─→ WishlistItem (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ userUuid (FK)
        │   ├─ productUuid
        │   └─ addedAt
        │
        ├─→ Notification (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ userUuid (FK)
        │   ├─ type (ORDER, REVIEW, LOYALTY, SHIPMENT, SUPPORT)
        │   ├─ title
        │   ├─ message
        │   ├─ isRead
        │   └─ createdAt
        │
        ├─→ LoyaltyTransaction (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ userUuid (FK)
        │   ├─ type (EARNED, REDEEMED, EXPIRED)
        │   ├─ points
        │   ├─ orderUuid (optional)
        │   └─ transactionDate
        │
        ├─→ SupportTicket (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ userUuid (FK)
        │   ├─ subject
        │   ├─ status (OPEN, IN_PROGRESS, RESOLVED, CLOSED)
        │   ├─ priority (LOW, MEDIUM, HIGH, URGENT)
        │   ├─ createdAt
        │   └─ resolvedAt
        │
        ├─→ SupportTicketComment (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ ticketUuid (FK)
        │   ├─ userUuid (FK) [buyer or support agent]
        │   ├─ message
        │   └─ createdAt
        │
        ├─→ OTPVerification (one-to-many)
        │   ├─ otp
        │   ├─ expiryTime
        │   └─ type (EMAIL, SMS)
        │
        └─→ PasswordRecovery (one-to-many)
            ├─ uuid (PK)
            ├─ userUuid (FK)
            ├─ resetToken (unique)
            ├─ expiryTime (1 hour)
            └─ usedAt (null until used)

AUTH-DB:
  RefreshToken
  ├─ token (PK, unique, uuid)
  ├─ userUuid (FK → User)
  ├─ expiryDate
  └─ revoked (for rotation pattern)

PRODUCT-DB:
  ┌──────────────────────┐         ┌──────────────┐
  │  Product             │◄────────│   Category   │
  ├──────────────────────┤ N:1     ├──────────────┤
  │ uuid (PK)            │         │ uuid (PK)    │
  │ sellerUuid           │─→ (FK)  │ name         │
  │ name                 │         │ slug         │
  │ description          │         │ parentId (FK)│ (for hierarchy)
  │ categoryId           │─────────→ status       │
  │ price                │         └──────────────┘
  │ stock                │
  │ status               │ (ACTIVE, INACTIVE, LISTED)
  │ avgRating            │
  │ reviewCount          │
  │ createdAt            │
  └──────────────────────┘
        │
        ├─→ ProductImage (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ productUuid (FK)
        │   ├─ imageUrl
        │   ├─ altText
        │   ├─ displayOrder
        │   └─ createdAt
        │
        ├─→ ProductVariant (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ productUuid (FK)
        │   ├─ sku
        │   ├─ size (e.g., S, M, L, XL)
        │   ├─ color
        │   ├─ price (variant-specific)
        │   ├─ stock (variant-specific)
        │   └─ status
        │
        └─→ InventoryHistory (optional tracking)
            ├─ uuid (PK)
            ├─ productUuid (FK)
            ├─ actionType (CREATE, PURCHASE, RETURN, ADJUSTMENT)
            ├─ previousStock
            ├─ newStock
            ├─ referenceId (orderUuid, returnUuid, etc.)
            └─ timestamp

  ┌──────────────────────────┐
  │   FlashDeal              │
  ├──────────────────────────┤
  │ uuid (PK)                │
  │ productUuid              │─→ (FK to Product)
  │ discountPercent          │
  │ maxDiscountAmt           │
  │ validFrom                │
  │ validUntil               │
  │ limitedStock             │ (optional)
  │ soldCount                │
  │ status                   │ (ACTIVE, EXPIRED, PAUSED)
  └──────────────────────────┘

ORDER-DB:
  ┌──────────────────────┐         ┌──────────────┐
  │     Order            │◄────────│   OrderItem  │
  ├──────────────────────┤ 1:many  ├──────────────┤
  │ uuid (PK)            │         │ uuid (PK)    │
  │ buyerUuid            │         │ orderUuid    │
  │ totalAmount          │         │ productUuid  │
  │ discountAmount       │         │ sellerUuid   │
  │ finalAmount          │         │ quantity     │
  │ couponCode           │         │ price        │
  │ couponDiscount       │         │ createdAt    │
  │ loyaltyRedeemed      │         └──────────────┘
  │ loyaltyDiscount      │
  │ status               │ (CREATED→CONFIRMED→SHIPPED→DELIVERED→RETURNED)
  │ paymentStatus        │ (PENDING→SUCCESS/FAILED)
  │ returnStatus         │ (PENDING→APPROVED/REJECTED→REFUNDED)
  │ shippingStatus       │ (CREATED→IN_TRANSIT→OUT_FOR_DELIVERY→DELIVERED)
  │ createdAt            │
  │ deliveredAt          │ (nullable)
  │ isDeleted            │ (soft-delete)
  └──────────────────────┘
        │
        ├─→ Coupon (one-to-many CouponUsage)
        │   ├─ uuid (PK)
        │   ├─ code (unique)
        │   ├─ type (PERCENTAGE, FIXED)
        │   ├─ discountValue
        │   ├─ maxDiscountAmount
        │   ├─ minOrderAmount
        │   ├─ validFrom
        │   ├─ validUntil
        │   ├─ maxTotalUsage
        │   ├─ maxUsagePerUser
        │   ├─ usageCount
        │   ├─ status (ACTIVE, INACTIVE, EXPIRED)
        │   └─ createdAt
        │
        ├─→ CouponUsage (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ orderUuid (FK)
        │   ├─ couponCode
        │   ├─ buyerUuid
        │   ├─ discountAmount
        │   └─ appliedAt
        │
        ├─→ ReturnRequest (one-to-one)
        │   ├─ uuid (PK)
        │   ├─ orderUuid (FK, unique)
        │   ├─ reason (DEFECTIVE, NOT_AS_DESCRIBED, CHANGED_MIND, etc.)
        │   ├─ comments
        │   ├─ status (PENDING→APPROVED/REJECTED→REFUNDED)
        │   ├─ refundAmount
        │   ├─ rejectionReason (if rejected)
        │   ├─ createdAt
        │   ├─ reviewedAt (nullable)
        │   └─ refundedAt (nullable)
        │
        ├─→ ShipmentTracking (one-to-many)
        │   ├─ uuid (PK)
        │   ├─ orderUuid (FK)
        │   ├─ location (currentLocation)
        │   ├─ latitude (nullable)
        │   ├─ longitude (nullable)
        │   ├─ status (CREATED→IN_TRANSIT→OUT_FOR_DELIVERY→DELIVERED)
        │   ├─ description (e.g., "Package in transit to Mumbai")
        │   ├─ timestamp
        │   └─ createdAt
        │
        ├─→ OrderInvoice (one-to-one)
        │   ├─ uuid (PK)
        │   ├─ orderUuid (FK)
        │   ├─ invoiceNumber (unique, e.g., INV-2026-001)
        │   ├─ invoicePath (PDF storage path)
        │   ├─ totalAmount
        │   ├─ discounts
        │   ├─ taxes
        │   ├─ issueDate
        │   ├─ dueDate
        │   ├─ status (GENERATED, SENT, VIEWED)
        │   └─ createdAt
        │
        └─→ AuditLog (one-to-many)
            ├─ uuid (PK)
            ├─ orderUuid (FK)
            ├─ actionType (CREATE, STATUS_CHANGE, RETURN_REQUEST, REFUND, etc.)
            ├─ actorUuid (who performed action)
            ├─ actorRole (BUYER, ADMIN, SELLER)
            ├─ details (JSON: old values, new values)
            ├─ ipAddress
            └─ timestamp

PAYMENT-DB:
  ┌──────────────┐         ┌─────────────────┐
  │   Payment    │◄────────│  PaymentSplit   │
  ├──────────────┤ 1:many  ├─────────────────┤
  │ uuid (PK)    │         │ uuid (PK)       │
  │ orderUuid    │         │ paymentUuid (FK)│
  │ buyerUuid    │         │ sellerUuid      │
  │ amount       │         │ amount          │
  │ status       │         │ refundedAmount  │
  │ createdAt    │         │ status          │
  └──────────────┘         │ createdAt       │
                           └─────────────────┘

  ProcessedEvent (idempotency)
  ├─ eventId (PK, "payment-completed:{paymentUuid}")
  ├─ eventType
  └─ createdAt

REVIEW-DB:
  ┌──────────────┐         ┌───────────────────┐
  │   Review     │◄────────│   ReviewImage     │
  ├──────────────┤ 1:many  ├───────────────────┤
  │ uuid (PK)    │         │ uuid (PK)         │
  │ orderUuid    │         │ reviewUuid (FK)   │
  │ productUuid  │         │ imageUrl          │
  │ sellerUuid   │         │ displayOrder      │
  │ buyerUuid    │         │ createdAt         │
  │ rating       │ (1-5)   └───────────────────┘
  │ comment      │
  │ helpfulCount │
  │ unhelpfulCnt │
  │ createdAt    │
  │ updatedAt    │
  │ isDeleted    │ (soft-delete)
  └──────────────┘
        │
        └─→ ReviewVote (one-to-many)
            ├─ uuid (PK)
            ├─ reviewUuid (FK)
            ├─ voterUuid (FK to user)
            ├─ voteType (HELPFUL, UNHELPFUL)
            └─ createdAt

  Index on (productUuid, buyerUuid) for unique constraint on Review
```

### Migrations

Each service uses Flyway for schema versioning:
```
V1__initial_schema.sql
  ├─ user-service: User, Address, CartItem, WishlistItem, Notification, LoyaltyPoint, LoyaltyTransaction, SupportTicket, SupportTicketComment, SellerDetail, OTPVerification, PasswordRecovery tables
  ├─ auth-service: RefreshToken table
  ├─ product-service: Product, Category, ProductImage, ProductVariant, FlashDeal, InventoryHistory tables
  ├─ order-service: Order, OrderItem, Coupon, CouponUsage, ReturnRequest, ShipmentTracking, OrderInvoice, AuditLog tables
  ├─ payment-service: Payment, PaymentSplit, ProcessedEvent tables
  └─ review-service: Review, ReviewImage, ReviewVote tables

V2__feature_additions.sql
  ├─ order-service: Enhanced return and shipment tables
  ├─ payment-service: Refund enhancements
  └─ product-service: Flash deals and inventory tracking
```

---

## Event-Driven Architecture Enhancements

### Kafka Topics & Events (2026 Addition)

```
KAFKA TOPICS AND EVENT SCHEMA:

1. order.created
   Topic: order.created
   Partition Key: buyerUuid (ensures ordering per buyer)
   Schema:
   {
     "orderUuid": "abc-123",
     "buyerUuid": "buyer-uuid",
     "amount": 2000.00,
     "items": [
       {"productUuid": "prod-uuid", "sellerUuid": "seller-uuid", "quantity": 1, "price": 1000.00}
     ],
     "addressUuid": "addr-uuid",
     "timestamp": 1708857600000
   }
   Consumers: payment-service (initiate payment)
   TTL: 7 days

2. payment.completed
   Topic: payment.completed
   Partition Key: orderUuid
   Schema:
   {
     "paymentUuid": "pay-uuid",
     "orderUuid": "order-uuid",
     "status": "SUCCESS|FAILED",
     "amount": 2000.00,
     "message": (if failed)
     "timestamp": 1708857600000
   }
   Consumers: order-service (update order status)
   TTL: 7 days

3. order.return.status.changed (NEW - 2026)
   Topic: order.return.status.changed
   Partition Key: orderUuid
   Schema:
   {
     "returnRequestUuid": "return-uuid",
     "orderUuid": "order-uuid",
     "buyerUuid": "buyer-uuid",
     "status": "PENDING|APPROVED|REJECTED|REFUNDED",
     "refundAmount": 2000.00 (if approved),
     "reason": "Approval/Rejection reason",
     "timestamp": 1708857600000
   }
   Consumers: 
     - order-service (update return status)
     - payment-service (initiate refund if approved)
     - notification-service (send notification)
   TTL: 30 days

4. notification.created (NEW - 2026)
   Topic: notification.created
   Partition Key: userUuid
   Schema:
   {
     "notificationUuid": "notif-uuid",
     "userUuid": "user-uuid",
     "type": "ORDER|REVIEW|LOYALTY|SHIPMENT|SUPPORT|PAYMENT",
     "title": "Your order has been delivered",
     "message": "Order #12345 has been delivered",
     "actionUrl": "/orders/order-uuid",
     "channels": ["IN_APP", "EMAIL", "SMS"],
     "timestamp": 1708857600000
   }
   Consumers: 
     - user-service (save in-app notification)
     - email-service (send email)
     - sms-service (send SMS)
   TTL: 90 days

5. loyalty.points.earned (NEW - 2026)
   Topic: loyalty.points.earned
   Partition Key: buyerUuid
   Schema:
   {
     "transactionUuid": "trans-uuid",
     "buyerUuid": "buyer-uuid",
     "orderUuid": "order-uuid",
     "points": 250,
     "pointsPerRupee": 1.0,
     "totalAmount": 2000.00,
     "timestamp": 1708857600000
   }
   Consumers: 
     - user-service (credit loyalty points)
     - notification-service (send point earned notification)
   TTL: 7 days

6. inventory.updated (NEW - 2026)
   Topic: inventory.updated
   Partition Key: productUuid
   Schema:
   {
     "inventoryHistoryUuid": "history-uuid",
     "productUuid": "product-uuid",
     "actionType": "PURCHASE|RETURN|ADJUSTMENT|RESTOCK",
     "previousStock": 100,
     "newStock": 99,
     "quantityChanged": 1,
     "referenceId": "order-uuid|return-uuid",
     "timestamp": 1708857600000
   }
   Consumers: 
     - product-service (update product stock cache)
     - analytics-service (track inventory trends)
   TTL: 365 days (for historical analysis)

EVENT ORDERING GUARANTEES:
  - All events for same partition key are processed in order
  - order.created → payment.completed → order.return.status.changed (same partition)
  - Ensures causality for single buyer/order

IDEMPOTENCY HANDLING:
  - Each consumer tracks ProcessedEvent table
  - eventId = "{topicName}:{uuid}:{sequence}"
  - Duplicate events are detected and skipped
  - Prevents double-crediting loyalty or double-refunding
```

### Event Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    2026 EVENT-DRIVEN ARCHITECTURE                            │
├──────────────────────────────────────────────────────────────────────────────┤

ORDER SERVICE
  │
  ├─→ Creates Order
  │   └─ Publishes: order.created
  │
  ├─→ Receives: payment.completed
  │   └─ Updates order.paymentStatus
  │
  ├─→ Receives: order.return.status.changed (status=APPROVED)
  │   └─ Updates order.status = RETURNED, initiates refund workflow
  │
  └─→ Generates: AuditLog for every status change

              ↓

PAYMENT SERVICE
  │
  ├─→ Consumes: order.created
  │   ├─ Validates order amount
  │   ├─ Simulates payment processing
  │   └─ Publishes: payment.completed (status=SUCCESS/FAILED)
  │
  ├─→ Consumes: order.return.status.changed (status=APPROVED)
  │   └─ Creates Refund transaction
  │       ├─ Publishes: refund.initiated
  │       └─ Updates PaymentSplit: refundedAmount
  │
  └─→ Updates: seller dashboards with earnings summary

              ↓

USER SERVICE
  │
  ├─→ Consumes: loyalty.points.earned
  │   ├─ Credits LoyaltyTransaction
  │   ├─ Updates user.loyaltyBalance
  │   ├─ Checks tier eligibility (BRONZE→SILVER→GOLD)
  │   └─ Publishes: loyalty.tier.changed (if upgraded)
  │
  ├─→ Consumes: notification.created
  │   ├─ Saves Notification entity
  │   ├─ Marks as unread
  │   └─ Updates notification count for badge
  │
  └─→ Consumes: inventory.updated
       └─ Refreshes product cache if in wishlist

              ↓

NOTIFICATION SERVICE (new microservice)
  │
  ├─→ Consumes: order.created
  │   ├─ Publishes: notification.created (type=ORDER_CONFIRMED)
  │   └─ Templates: "Order Confirmed - {{orderUuid}}"
  │
  ├─→ Consumes: loyalty.points.earned
  │   ├─ Publishes: notification.created (type=LOYALTY)
  │   └─ Templates: "Earned {{points}} loyalty points!"
  │
  ├─→ Consumes: order.return.status.changed
  │   ├─ Publishes: notification.created (type=RETURN)
  │   └─ Templates: "Return {{status}}: {{reason}}"
  │
  └─→ Manages: notification.created → user-service, email, SMS
      (delivery channel selection based on user preferences)

              ↓

PRODUCT SERVICE
  │
  ├─→ Consumes: inventory.updated
  │   ├─ Updates product.stock cache
  │   ├─ Checks if stock < minThreshold
  │   └─ Publishes: inventory.low.stock.alert (if low)
  │
  └─→ Consumes: review.submitted
       ├─ Recalculates product.avgRating
       ├─ Increments product.reviewCount
       └─ Invalidates Redis cache

              ↓

REVIEW SERVICE
  │
  ├─→ Creates Review
  │   └─ Publishes: review.submitted
  │
  └─→ Consumes: order.delivered (triggers notification)
       └─ Enable review submission for buyer
```

---

## API Layers & Security

### Security Layers

```
┌──────────────────────────────────────────────────────────┐
│ 1. EXTERNAL (Client facing via API Gateway)              │
│    - JWT Token Validation (HS256 signature check)        │
│    - Rate Limiting (RateLimitGlobalFilter)              │
│    - Correlation ID Injection                           │
├──────────────────────────────────────────────────────────┤
│ 2. INTERNAL (Service-to-Service)                         │
│    - X-Internal-Secret Header (InternalSecretFilter)    │
│    - Feign Client Interceptors                          │
│    - Circuit Breaker (Resilience4j)                     │
├──────────────────────────────────────────────────────────┤
│ 3. METHOD LEVEL                                          │
│    - @PreAuthorize("hasRole('SELLER')")               │
│    - Custom @Secured annotations                        │
│    - Authorization logic in service layer                │
├──────────────────────────────────────────────────────────┤
│ 4. DATABASE LEVEL                                        │
│    - Row-level security via userUuid checks             │
│    - Soft deletes (isDeleted flag)                      │
│    - Audit timestamps (createdAt, updatedAt)            │
└──────────────────────────────────────────────────────────┘
```

### JWT Token Structure

```
Access Token (Header.Payload.Signature):
{
  "alg": "HS256",        // Header
  "typ": "JWT"
}
{
  "sub": "user@example.com",  // Subject (email)
  "uuid": "abc-123",           // User unique ID
  "role": "BUYER",             // Role for authorization
  "iat": 1708857600,           // Issued at timestamp
  "exp": 1708858500            // Expiration (15 mins later)
}
Signature = HMAC-SHA256(header + payload, secret_key)

Usage: Authorization: Bearer {access_token}
Validation:
  1. Split on '.' to get 3 parts: header, payload, signature
  2. Verify signature matches HMAC(header.payload)
  3. Decode payload, check exp < now
  4. Extract UUID, role for authorization
```

---

## Development Guide

### Project Structure

```
ecommerce-backend/
├── auth-service/              # JWT issuance & refresh
│   ├── src/main/java/
│   │   └── com/sourabh/auth_service/
│   │       ├── controller/    # REST endpoints
│   │       ├── service/       # Business logic
│   │       ├── entity/        # JPA entities
│   │       ├── repository/    # Data access
│   │       ├── security/      # JWT filters
│   │       ├── config/        # Spring configs
│   │       ├── util/          # JWT utility
│   │       └── exception/     # Custom exceptions
│   └── src/main/resources/
│       ├── application.properties  # Config from config-server
│       └── db/migration/           # Flyway migrations
│
├── user-service/              # User profiles & seller verification
│   ├── src/main/java/
│   │   └── com/sourabh/user_service/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── entity/        # User, Address, Cart, Wishlist, etc.
│   │       ├── repository/
│   │       ├── config/
│   │       ├── security/      # Interceptors
│   │       └── exception/
│   └── src/main/resources/
│
├── product-service/           # Product catalog & inventory
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/         # Unit & integration tests
│
├── order-service/             # Orders, returns, shipments
│   ├── src/main/java/
│   │   └── com/sourabh/order_service/
│   │       ├── kafka/         # Kafka consumers
│   │       │   ├── consumer/  # PaymentEventConsumer, ReturnProcessor
│   │       │   └── event/     # OrderCreatedEvent, OrderItemEvent
│   │       ├── feign/         # RestClient to product-service
│   │       ├── entity/        # Order, OrderItem, Coupon, Return, etc.
│   │       └── ... (standard structure)
│   └── src/test/java/
│
├── payment-service/           # Payment processing & splits
│   ├── src/main/java/
│   │   └── com/sourabh/payment_service/
│   │       ├── kafka/
│   │       │   ├── consumer/  # OrderEventConsumer
│   │       │   └── event/     # PaymentCompletedEvent
│   │       ├── entity/        # Payment, PaymentSplit
│   │       └── ... (standard structure)
│   └── src/test/java/
│
├── review-service/            # Product reviews & ratings
│   ├── src/main/java/
│   └── src/test/java/
│
├── notification-service/      # Notifications (NEW 2026)
│   ├── src/main/java/
│   └── src/test/java/
│
├── api-gateway/               # Spring Cloud Gateway
│   ├── security/              # JWT validation
│   ├── filter/                # Custom global filters
│   └── config/                # Route definitions
│
├── eureka-server/             # Service discovery
├── config-server/             # Centralized config
├── docker-compose.yml         # All services, DB, Kafka, etc.
├── pom.xml                    # Parent POM for all services
└── README.md
```

### Running the Application

```bash
# Start all services (18+ containers)
docker-compose up --build

# Services available at:
# http://localhost:8080          (API Gateway)
# http://localhost:8761          (Eureka)
# http://localhost:8888          (Config Server)  
# http://localhost:9090          (Prometheus)
# http://localhost:3000          (Grafana)
# http://localhost:9411          (Zipkin)

# Build individual service
cd payment-service
mvn clean package

# Run tests for service
mvn test

# See logs of specific service
docker logs -f ecommerce-backend-payment-service-1
```

### Key Configuration Files

```properties
# config-server/src/main/resources/config/
├── auth-service.properties
│   ├── jwt.secret=<256-bit-key>          # Signing secret
│   ├── jwt.access-token-expiration=900000
│   ├── jwt.refresh-token-expiration=604800000
│   └── internal.secret=<internal-api-key>
│
├── order-service.properties
│   ├── spring.kafka.bootstrap-servers=kafka:9092
│   ├── service.product.url=http://product-service:8080
│   └── order.return.window.days=30
│
├── payment-service.properties
│   ├── payment.platform-fee-percent=10.0
│   └── payment.delivery-fee-per-item=30.0
│
└── spring.cloud.config.server.git.uri=<github-config-repo>
```

---

## Testing Strategy

### Unit Tests
- **Mocking**: Mockito for dependencies
- **Assertions**: AssertJ for fluent assertions
- **Test Naming**: `{method}_{scenario}_{expected_result}`
- **Example**:
  ```java
  @Test
  @DisplayName("login: fails — invalid password")
  void login_invalidPassword_throwsAuthException() {
      // Arrange
      LoginRequest request = new LoginRequest();
      request.setEmail("user@test.com");
      request.setPassword("wrongPassword");
      
      // Act & Assert
      assertThatThrownBy(() -> authService.login(request))
          .isInstanceOf(AuthException.class)
          .hasMessageContaining("Invalid credentials");
  }
  ```

### Integration Tests
- **Testcontainers**: Real PostgreSQL database
- **@DataJpaTest**: Only loads JPA configuration
- **Example**:  
  ```java
  @DataJpaTest
  @Testcontainers
  class OrderRepositoryIntegrationTest {
      @Container
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
      
      @Autowired
      private OrderRepository orderRepository;
      
      @Test
      void findByBuyer_returnsOnlyBuyersOrders() { ... }
  }
  ```

### Test Coverage Goals
- **Controllers**: 80%+ (all paths, error cases)
- **Services**: 85%+ (business logic, state transitions)
- **Repositories**: 70%+ (query methods, edge cases)
- **Utils**: 90%+ (utility functions used everywhere)

---

## Important Concepts

### 1. **Saga Pattern** (Distributed Transactions)
```
Order transaction spans 2 services (Order + Payment):
Option 1: Distributed 2PC (locks, slow) ❌
Option 2: Saga with Kafka events (async, compensating) ✅

Happy Path:
  order-service creates Order → publishes order.created
  payment-service creates Payment → publishes payment.completed
  order-service updates paymentStatus

Compensating Path (if payment fails):
  order-service cancels Order
  order-service calls product-service to restore stock
  Ensures no orphaned orders or inventory issues
```

### 2. **Idempotency** (Prevent Duplicate Processing)
```
Problem: Kafka message reprocessed due to network retry
Solution: ProcessedEvent table tracks {eventId, eventType, createdAt}

Payment consumer checks:
  if (processedEventRepository.existsByEventId("payment-completed:{paymentUuid}"))
    return;  // Already processed, skip
  
// Process new event
process(event);
processedEventRepository.save(new ProcessedEvent(eventId));
```

### 3. **Circuit Breaker** (Resilience)
```
Problem: order-service calls product-service, product-service down
Solution: Resilience4j circuit breaker

States:
  - CLOSED: Normal operation, requests pass through
  - OPEN: Failures detected, fast-fail without calling service
  - HALF_OPEN: Try a few requests to see if service recovered

Config:
  failureRateThreshold=${resilience4j.circuitbreaker.instances.product-service.failure-rate-threshold}
  slidingWindowSize=${resilience4j.circuitbreaker.instances.product-service.sliding-window-size}
```

### 4. **JWT vs RefreshToken**
```
Access Token:
  - Stateless (no DB lookup)
  - Short-lived (15 min)
  - Cannot be revoked (design tradeoff)
  - Signed with secret key

Refresh Token:
  - Stateful (stored in DB)
  - Long-lived (7 days)
  - Revocable (logout)
  - Rotation pattern (old revoked when new issued)
  - Used to get new access token
```

### 5. **Soft Deletes** (Data Retention)
```
Instead of DELETE, set isDeleted=true
Benefits:
  - Audit trail (know what was deleted and when)
  - Referential integrity (foreign keys still valid)
  - Recovery possible (restore old records)

Usage:
  SELECT * FROM orders WHERE isDeleted = false
  (always include this filter in queries)
```

---

## Common Workflows

### Adding New API Endpoint

1. **Define DTO** (Data Transfer Object)
   ```java
   public class CreateProductRequest {
       @NotBlank
       private String name;
       
       @Positive
       private Double price;
   }
   ```

2. **Add Controller Method**
   ```java
   @PostMapping("/product")
   @PreAuthorize("hasRole('SELLER')")
   public ResponseEntity<ProductResponse> createProduct(
       @Valid @RequestBody CreateProductRequest request,
       @RequestHeader("X-User-Uuid") String sellerUuid) {
       ProductResponse response = productService.createProduct(request, sellerUuid);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
   }
   ```

3. **Implement Service Logic**
   ```java
   public ProductResponse createProduct(CreateProductRequest request, String sellerUuid) {
       Product product = Product.builder()
           .uuid(UUID.randomUUID().toString())
           .sellerUuid(sellerUuid)
           .name(request.getName())
           .price(request.getPrice())
           .status(ProductStatus.ACTIVE)
           .build();
       
       productRepository.save(product);
       return ProductResponse.from(product);
   }
   ```

4. **Add Repository Method** (if needed)
   ```java
   public interface ProductRepository extends JpaRepository<Product, Long> {
       Page<Product> findBySellerUuid(String sellerUuid, Pageable pageable);
   }
   ```

5. **Add Tests**
   ```java
   @Test
   @DisplayName("createProduct: seller creates valid product")
   void createProduct_success() {
       CreateProductRequest req = new CreateProductRequest();
       req.setName("Test Product");
       req.setPrice(100.0);
       
       ProductResponse response = productService.createProduct(req, "seller-uuid");
       
       assertThat(response.getName()).isEqualTo("Test Product");
       verify(productRepository).save(any(Product.class));
   }
   ```

---

## Troubleshooting

### Payment Status Stays "PENDING"
**Cause**: Kafka events not processed (check logs)
```bash
# Check payment-service logs for Kafka errors
docker logs ecommerce-backend-payment-service-1 | grep -i kafka

# Check consumer lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group order-service \
  --describe
```

### Order Creation Fails with "product not found"
**Cause**: No products in database
```bash
# Create test product via API
curl -X POST http://localhost:8080/api/product \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "price": 100}'
```

### JWT Token Validation Fails at Gateway
**Cause**: Secret key mismatch between auth-service and gateway
```bash
# Verify config-server is serving correct secret
curl http://localhost:8888/auth-service/master \
  | grep jwt.secret
```

---

## Next Steps for New Developers

1. **Read this guide completely** (you are here ✅)
2. **Review flow diagrams** (understand request paths)
3. **Examine test classes** (see expected behavior)
4. **Read service code** starting with controllers → services
5. **Trace example request** (login → create order → payment)
6. **Fork, clone, run locally** (`docker-compose up`)
7. **Make small changes**, run tests, verify behavior
8. **Ask questions**, join team discussions

---

**Last Updated**: February 2026 (2026 Architecture Enhancements v1.1)  
**Version**: 1.1 - Complete Backend Documentation with 2026 Features

### 2026 Update Summary
This version includes comprehensive enhancements across all services:
- **User Service**: Password recovery, address management, shopping cart, wishlist, notifications, loyalty program, support tickets
- **Product Service**: Category management, product images, variants, flash deals, real-time inventory tracking
- **Order Service**: Coupon system, return management, shipment tracking, invoice generation, financial dashboards, audit logging
- **Review Service**: Image uploads, community voting on review helpfulness
- **Event-Driven**: New Kafka events for returns, notifications, loyalty, and inventory updates
- **Architecture**: Enhanced database schemas, new API endpoints, comprehensive flow diagrams
- **Notification Service**: New service for multi-channel notifications
