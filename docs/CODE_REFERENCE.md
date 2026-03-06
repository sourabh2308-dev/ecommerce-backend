# Code Reference

Complete file-by-file reference for every service in the SourHub e-commerce platform. This document lists every class, its package, its purpose, and its key methods.

---

## Table of Contents

- [API Gateway](#api-gateway)
- [Auth Service](#auth-service)
- [User Service](#user-service)
- [Product Service](#product-service)
- [Order Service](#order-service)
- [Payment Service](#payment-service)
- [Review Service](#review-service)
- [Eureka Server](#eureka-server)
- [Config Server](#config-server)

---

## API Gateway

**Module:** `api-gateway/`  
**Base package:** `com.ecommerce.apigateway`

| File | Package | Purpose |
|------|---------|---------|
| `ApiGatewayApplication.java` | `.` | Spring Boot entry point |
| `SecurityConfig.java` | `.config` | WebFlux security — defines public/protected paths, integrates JWT filter |
| `JwtAuthenticationFilter.java` | `.filter` | Extracts Bearer token, validates JWT, sets security context |
| `JwtAuthenticationConverter.java` | `.filter` | Converts HTTP request to Authentication token |
| `JwtAuthenticationManager.java` | `.filter` | ReactiveAuthenticationManager — validates JWT claims |
| `RateLimitGlobalFilter.java` | `.filter` | Token-bucket rate limiting per IP using Bucket4j |
| `CorrelationIdGlobalFilter.java` | `.filter` | Generates/passes X-Correlation-ID header |
| `InternalSecretFilterConfig.java` | `.filter` | Injects X-Internal-Secret header into downstream requests |
| `JwtUtil.java` | `.util` | JWT parsing, validation, claim extraction (HS256) |
| `application.properties` | `resources` | Route definitions, JWT secret, server config |

### Key Configuration
```properties
# Route mappings
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://auth-service:8080
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**
# ... similar for user, product, order, review, payment
```

---

## Auth Service

**Module:** `auth-service/`  
**Base package:** `com.ecommerce.authservice`

### Controllers

| File | Path Prefix | Endpoints |
|------|-------------|-----------|
| `AuthController.java` | `/api/auth` | login, refresh, logout, forgot-password, reset-password |

### Services

| File | Purpose | Key Methods |
|------|---------|-------------|
| `AuthService.java` | Interface | `login()`, `refreshToken()`, `logout()`, `forgotPassword()`, `resetPassword()` |
| `AuthServiceImpl.java` | Implementation | Credential validation via user-service Feign, JWT generation, refresh token rotation |
| `JwtUtil.java` | JWT utility | `generateAccessToken()`, `extractEmail()`, `extractUserUuid()`, `extractRole()`, `validateToken()` |

### Entities

| File | Table | Key Fields |
|------|-------|------------|
| `RefreshToken.java` | `refresh_tokens` | id, token (UUID), userUuid, expiryDate, revoked |

### DTOs

| File | Fields |
|------|--------|
| `LoginRequest.java` | email, password |
| `AuthResponse.java` | accessToken, refreshToken, tokenType |
| `ForgotPasswordRequest.java` | email |
| `ResetPasswordRequest.java` | email, otpCode, newPassword |
| `UserDto.java` | User data from user-service (uuid, email, password hash, role, status) |

### Repositories

| File | Entity | Custom Queries |
|------|--------|----------------|
| `RefreshTokenRepository.java` | RefreshToken | `findByToken()`, `deleteByUserUuid()` |

### Configuration

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Disables CSRF, permits all (gateway handles auth) |
| `FeignConfig.java` | Feign client config for user-service calls |
| `InternalFeignInterceptor.java` | Adds X-Internal-Secret header to Feign requests |
| `UserServiceClient.java` | Feign interface for user-service internal endpoints |
| `application.properties` | JWT secret, token expiration, datasource, Eureka |

---

## User Service

**Module:** `user-service/`  
**Base package:** `com.ecommerce.userservice`

### Controllers

| File | Path Prefix | Endpoints |
|------|-------------|-----------|
| `UserController.java` | `/api/user` | register, verify-otp, resend-otp, me, change-password, seller-details, internal |
| `AddressController.java` | `/api/user/me/addresses` | CRUD, set default |
| `CartController.java` | `/api/user/cart` | get, add, update qty, remove, clear |
| `WishlistController.java` | `/api/user/wishlist` | get, add, remove, check |
| `NotificationController.java` | `/api/user/notifications` | list, unread, count, mark-read |
| `LoyaltyController.java` | `/api/user/loyalty` | balance, history, redeem, award |
| `SupportTicketController.java` | `/api/user/support` | CRUD, messages, assign, close |
| `AdminController.java` | `/api/user/admin` | list users, search, block, unblock, approve |

### Services

| File | Purpose |
|------|---------|
| `UserService.java` / `UserServiceImpl.java` | User lifecycle: register, verify, profile, password, seller details |
| `AddressService.java` / `AddressServiceImpl.java` | Address CRUD with default management |
| `CartService.java` / `CartServiceImpl.java` | Cart operations, quantity management |
| `WishlistService.java` / `WishlistServiceImpl.java` | Wishlist add/remove/check |
| `NotificationService.java` / `NotificationServiceImpl.java` | Notification CRUD, mark read |
| `LoyaltyService.java` / `LoyaltyServiceImpl.java` | Points balance, earn, redeem, tier calculation |
| `SupportTicketService.java` / `SupportTicketServiceImpl.java` | Ticket lifecycle, message threading |
| `EmailService.java` | OTP email sending, invoice email |
| `OTPService.java` | OTP generation (6-digit), validation, expiry |

### Entities

| File | Table | Key Fields |
|------|-------|------------|
| `User.java` | `users` | id, uuid, firstName, lastName, email, password, role, status, emailVerified, isApproved, lastLoginAt |
| `Address.java` | `addresses` | id, uuid, userId, fullName, phone, addressLine1/2, city, state, pincode, isDefault |
| `CartItem.java` | `cart_items` | id, uuid, userId, productUuid, productName, productImage, price, quantity |
| `WishlistItem.java` | `wishlist_items` | id, uuid, userId, productUuid, productName, productImage, price |
| `OTPVerification.java` | `otp_verifications` | id, email, otpCode, type (EMAIL_VERIFICATION/PASSWORD_RESET), expiryTime, isUsed |
| `Notification.java` | `notifications` | id, uuid, userId, title, message, type, isRead, createdAt |
| `LoyaltyPoint.java` | `loyalty_points` | id, uuid, userId, pointsBalance, tier |
| `LoyaltyTransaction.java` | `loyalty_transactions` | id, uuid, userId, transactionType, amount, reason, reference |
| `SupportTicket.java` | `support_tickets` | id, uuid, userId, subject, description, category, status, priority, assignedAdminUuid |
| `SupportMessage.java` | `support_messages` | id, ticketId, senderUuid, senderRole, message, attachmentUrl |
| `SellerDetail.java` | `seller_details` | id, uuid, userId, businessName, businessType, gstNumber, panNumber, bankAccount, ifsc |

### DTOs

| File | Direction | Key Fields |
|------|-----------|------------|
| `RegisterRequest.java` | Request | firstName, lastName, email, password, phoneNumber, role |
| `VerifyOTPRequest.java` | Request | email, otpCode |
| `UpdateProfileRequest.java` | Request | firstName, lastName, phoneNumber |
| `ChangePasswordRequest.java` | Request | currentPassword, newPassword, confirmNewPassword |
| `SellerDetailRequest.java` | Request | businessName, businessType, gstNumber, panNumber, bank details |
| `UserResponse.java` | Response | uuid, name, email, phone, role, status, emailVerified |
| `SellerDetailResponse.java` | Response | All seller detail fields + timestamps |
| `AddressRequest.java` | Request | label, fullName, phone, address, city, state, pincode, isDefault |
| `AddressResponse.java` | Response | uuid, label, fullName, address fields, isDefault, createdAt |
| `CartItemRequest.java` | Request | productUuid, productName, productImage, price, quantity |
| `CartResponse.java` | Response | items, totalItems, totalAmount |
| `WishlistRequest.java` | Request | productUuid, productName, productImage, price |
| `WishlistItemResponse.java` | Response | id, productUuid, productName, productImage, price, createdAt |
| `CreateTicketRequest.java` | Request | subject, description, category, orderUuid |
| `TicketResponse.java` | Response | uuid, subject, status, messages, timestamps |
| `NotificationResponse.java` | Response | uuid, title, message, type, isRead, createdAt |
| `LoyaltyBalanceResponse.java` | Response | userUuid, balance, expiringPoints |
| `InternalUserDto.java` | Response | Internal user data for cross-service calls |

### Repositories

| File | Entity |
|------|--------|
| `UserRepository.java` | User |
| `AddressRepository.java` | Address |
| `CartItemRepository.java` | CartItem |
| `WishlistItemRepository.java` | WishlistItem |
| `OTPVerificationRepository.java` | OTPVerification |
| `NotificationRepository.java` | Notification |
| `LoyaltyPointRepository.java` | LoyaltyPoint |
| `LoyaltyTransactionRepository.java` | LoyaltyTransaction |
| `SupportTicketRepository.java` | SupportTicket |
| `SupportMessageRepository.java` | SupportMessage |
| `SellerDetailRepository.java` | SellerDetail |

### Configuration

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Method-level security, BCrypt encoder |
| `RedisConfig.java` | Redis cache manager, TTL settings |
| `InternalSecretFilter.java` | Validates X-Internal-Secret on internal endpoints |
| `application.properties` | Datasource, Redis, Eureka, email config |

### Enums

| File | Values |
|------|--------|
| `Role.java` | ADMIN, SELLER, BUYER |
| `UserStatus.java` | PENDING_VERIFICATION, PENDING_DETAILS, PENDING_APPROVAL, ACTIVE, BLOCKED, DELETED |
| `NotificationType.java` | ORDER, REVIEW, LOYALTY, SUPPORT |
| `TicketStatus.java` | OPEN, IN_PROGRESS, RESOLVED, CLOSED |
| `TicketCategory.java` | PRODUCT, ORDER, PAYMENT, SHIPPING, OTHER |
| `OTPType.java` | EMAIL_VERIFICATION, PASSWORD_RESET |
| `PointsTransactionType.java` | EARN, REDEEM |

---

## Product Service

**Module:** `product-service/`  
**Base package:** `com.ecommerce.productservice`

### Controllers

| File | Path Prefix | Endpoints |
|------|-------------|-----------|
| `ProductController.java` | `/api/product` | CRUD, search, admin approve/block, internal stock |
| `CategoryController.java` | `/api/category` | CRUD, hierarchy, reorder |
| `FlashDealController.java` | `/api/product/deals` | create, active, my, delete |
| `ProductImageController.java` | `/api/product/{uuid}/images` | add, list, delete |
| `VariantController.java` | `/api/product/{uuid}/variants` | add, list, update, delete |

### Services

| File | Purpose |
|------|---------|
| `ProductService.java` / `ProductServiceImpl.java` | Product CRUD, stock management, approval workflow |
| `CategoryService.java` / `CategoryServiceImpl.java` | Category hierarchy, reordering |
| `FlashDealService.java` / `FlashDealServiceImpl.java` | Time-limited promotions |
| `ProductImageService.java` / `ProductImageServiceImpl.java` | Image management with ordering |
| `VariantService.java` / `VariantServiceImpl.java` | Size/color/SKU variant management |
| `ElasticsearchService.java` | Product index/search in Elasticsearch |
| `InventoryHistoryService.java` | Stock movement tracking |

### Entities

| File | Table | Key Fields |
|------|-------|------------|
| `Product.java` | `products` | id, uuid, name, description, price, stock, category, sellerUuid, status, averageRating, totalReviews, imageUrl, lowStockThreshold |
| `Category.java` | `categories` | id, uuid, name, parentCategoryId, displayOrder |
| `ProductImage.java` | `product_images` | id, uuid, productId, imageUrl, altText, displayOrder |
| `ProductVariant.java` | `product_variants` | id, uuid, productId, variantName, variantValue, sku, price, stock |
| `FlashDeal.java` | `flash_deals` | id, uuid, productId, discountPercent, startDateTime, endDateTime, isActive |
| `InventoryHistory.java` | `inventory_history` | id, productId, eventType, quantity, reason, relatedOrderId |

### DTOs

| File | Key Fields |
|------|------------|
| `CreateProductRequest.java` | name, description, price, stock, category, imageUrl |
| `UpdateProductRequest.java` | All fields optional |
| `ProductResponse.java` | uuid, name, description, price, stock, category, sellerUuid, status, rating, images |
| `CreateCategoryRequest.java` | name, description, imageUrl, parentUuid, displayOrder |
| `CategoryResponse.java` | uuid, name, description, parentUuid, children, displayOrder |
| `FlashDealRequest.java` | productUuid, discountPercent, startTime, endTime |
| `FlashDealResponse.java` | uuid, productUuid, discountPercent, startTime, endTime, isActive |
| `ImageRequest.java` | imageUrl, displayOrder, alt |
| `ImageResponse.java` | id, productUuid, imageUrl, displayOrder, alt |
| `VariantRequest.java` | variantName, variantValue, priceOverride, stock, sku |
| `VariantResponse.java` | uuid, variantName, variantValue, priceOverride, stock, sku |
| `CursorPageResponse.java` | content, hasNext, cursor (cursor-based pagination) |

### Enums

| File | Values |
|------|--------|
| `ProductStatus.java` | DRAFT, ACTIVE, BLOCKED, OUT_OF_STOCK |
| `InventoryEventType.java` | CREATED, PURCHASED, RETURNED, ADJUSTED |

---

## Order Service

**Module:** `order-service/`  
**Base package:** `com.ecommerce.orderservice`

### Controllers

| File | Path Prefix | Endpoints |
|------|-------------|-----------|
| `OrderController.java` | `/api/order` | create, list, details, update status, seller orders, sub-orders, group |
| `CouponController.java` | `/api/order/coupons` | create, validate, list, delete |
| `ReturnController.java` | `/api/order/returns` | create, list, approve, reject, update status |
| `ShipmentTrackingController.java` | `/api/order/tracking` | add event, get history |
| `InvoiceController.java` | `/api/order/{uuid}/invoice` | download PDF, email |
| `DashboardController.java` | `/api/order/dashboard` | admin KPIs, seller KPIs |
| `AuditLogController.java` | `/api/order/audit` | list, by actor, by resource |

### Services

| File | Purpose |
|------|---------|
| `OrderService.java` / `OrderServiceImpl.java` | Order creation, multi-seller splitting, status management |
| `CouponService.java` / `CouponServiceImpl.java` | Coupon CRUD, validation, discount calculation |
| `ReturnService.java` / `ReturnServiceImpl.java` | Return request lifecycle |
| `ShipmentTrackingService.java` / `ShipmentTrackingServiceImpl.java` | Tracking event management |
| `InvoiceService.java` / `InvoiceServiceImpl.java` | PDF generation (iText), email delivery |
| `DashboardService.java` / `DashboardServiceImpl.java` | KPI aggregation |
| `AuditLogService.java` / `AuditLogServiceImpl.java` | Audit trail with JSON state snapshots |
| `PaymentEventConsumer.java` | Kafka consumer for payment.completed events |
| `ProductServiceClient.java` | Feign client for product stock operations |

### Entities

| File | Table | Key Fields |
|------|-------|------------|
| `Order.java` | `orders` | id, uuid, buyerUuid, totalAmount, status, paymentStatus, orderType, parentOrderUuid, orderGroupId, shipping fields, couponCode, discountAmount, taxPercent, taxAmount, currency, returnType, returnReason |
| `OrderItem.java` | `order_items` | id, uuid, orderId, productUuid, sellerUuid, quantity, price, subTotal |
| `Coupon.java` | `coupons` | id, uuid, code, discountType, discountValue, minOrderAmount, maxDiscount, totalUsageLimit, perUserLimit, usedCount, validFrom, validUntil, sellerUuid |
| `ReturnRequest.java` | `return_requests` | id, uuid, orderId, type, reason, status, refundAmount, adminNotes |
| `ShipmentTracking.java` | `shipment_tracking` | id, uuid, orderId, status, location, carrierName, trackingNumber, description |
| `Invoice.java` | `invoices` | id, uuid, orderId, pdfPath, generatedAt, emailSentAt, status |
| `AuditLog.java` | `audit_logs` | id, resourceType, resourceId, action, actorUuid, actorRole, beforeState (JSON), afterState (JSON) |

### Enums

| File | Values |
|------|--------|
| `OrderStatus.java` | CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, RETURN_REQUESTED, PICKUP_SCHEDULED, PICKED_UP, RETURN_RECEIVED, EXCHANGE_ISSUED, REFUND_ISSUED, RETURN_REJECTED |
| `PaymentStatus.java` | PENDING, SUCCESS, FAILED, REFUNDED |
| `OrderType.java` | MAIN, SUB |
| `ReturnType.java` | REFUND, EXCHANGE |
| `ReturnStatus.java` | PENDING, APPROVED, REJECTED, PICKUP_SCHEDULED, PICKED_UP, RECEIVED, PROCESSED |

---

## Payment Service

**Module:** `payment-service/`  
**Base package:** `com.ecommerce.paymentservice`

### Controllers

| File | Path Prefix | Endpoints |
|------|-------------|-----------|
| `PaymentController.java` | `/api/payment` | initiate, list, details, by order, seller payments, webhook, dashboards |

### Services

| File | Purpose |
|------|---------|
| `PaymentService.java` / `PaymentServiceImpl.java` | Payment initiation, callback processing, saga coordination |
| `PaymentGateway.java` | Interface for payment gateway abstraction |
| `MockPaymentGateway.java` | Development — auto-success |
| `RazorpayGateway.java` | Production — Razorpay API integration |
| `OrderEventConsumer.java` | Kafka consumer for order.created events |
| `PaymentSplitService.java` | Multi-seller payment allocation |

### Entities

| File | Table | Key Fields |
|------|-------|------------|
| `Payment.java` | `payments` | id, uuid, orderUuid, buyerUuid, amount, gatewayOrderId, status |
| `PaymentSplit.java` | `payment_splits` | id, paymentId, sellerUuid, productUuid, itemAmount, platformFee, sellerAmount |
| `ProcessedEvent.java` | `processed_events` | id, eventId, eventType, processedAt |

### Enums

| File | Values |
|------|--------|
| `PaymentStatus.java` | INITIATED, PENDING, SUCCESS, FAILED |

---

## Review Service

**Module:** `review-service/`  
**Base package:** `com.ecommerce.reviewservice`

### Controllers

| File | Path Prefix | Endpoints |
|------|-------------|-----------|
| `ReviewController.java` | `/api/review` | create, update, delete, list by product, my reviews, vote, images |

### Services

| File | Purpose |
|------|---------|
| `ReviewService.java` / `ReviewServiceImpl.java` | Review CRUD, purchase verification, rating aggregation |
| `ReviewVoteService.java` / `ReviewVoteServiceImpl.java` | Helpful/unhelpful voting |
| `OrderServiceClient.java` | Feign client — verify purchase |
| `ProductServiceClient.java` | Feign client — update product rating |

### Entities

| File | Table | Key Fields |
|------|-------|------------|
| `Review.java` | `reviews` | id, uuid, productUuid, sellerUuid, buyerUuid, rating, comment, verifiedPurchase, isDeleted |
| `ReviewImage.java` | `review_images` | id, uuid, reviewId, imageUrl, displayOrder |
| `ReviewVote.java` | `review_votes` | id, uuid, reviewId, voterUuid, voteType |

---

## Eureka Server

**Module:** `eureka-server/`  
**Base package:** `com.ecommerce.eurekaserver`

| File | Purpose |
|------|---------|
| `EurekaServerApplication.java` | `@EnableEurekaServer` — Service discovery server |
| `application.properties` | Port 8761, self-registration disabled |

---

## Config Server

**Module:** `config-server/`  
**Base package:** `com.ecommerce.configserver`

| File | Purpose |
|------|---------|
| `ConfigServerApplication.java` | `@EnableConfigServer` — Centralized configuration |
| `application.properties` | Port 8888, config source settings |
