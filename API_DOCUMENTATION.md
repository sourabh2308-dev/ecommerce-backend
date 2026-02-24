# E-Commerce Backend - Complete API Documentation

## 📑 Table of Contents

1. [Overview](#overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Base URL & Headers](#base-url--headers)
4. [API Endpoints by Service](#api-endpoints-by-service)
5. [Request/Response Examples](#requestresponse-examples)
6. [Error Handling](#error-handling)
7. [Workflow Examples](#workflow-examples)

---

## Overview

This is a **production-ready microservices backend** for e-commerce with:
- 6 independent microservices (Auth, User, Product, Order, Payment, Review)
- API Gateway for routing and security
- Event-driven architecture (Kafka) for asynchronous operations
- Distributed transactions using Saga pattern
- Complete JWT-based authentication

**Base URL**: `http://localhost:8080/api` (through API Gateway)

---

## Authentication & Authorization

### JWT Token Structure

```
Header.Payload.Signature

Example Access Token Payload:
{
  "sub": "user@example.com",      // Subject (email)
  "uuid": "abc-123-def",          // User ID
  "role": "BUYER",                // Authorization role
  "iat": 1708857600,              // Issued at
  "exp": 1708858500               // Expires (15 minutes)
}
```

### Authorization Levels

| Level | Mechanism | Example |
|-------|-----------|---------|
| **Gateway** | JWT validation | Rejects invalid tokens |
| **Method** | @PreAuthorize Spring Security | `@PreAuthorize("hasRole('BUYER')")` |
| **Business Logic** | Service-layer checks | Buyer can only see own orders |
| **Database** | Soft deletes, row filters | `WHERE isDeleted = false` |

### Roles

- **BUYER**: Can place orders, leave reviews, view own orders
- **SELLER**: Can list products, view orders they fulfill, process payments
- **ADMIN**: Full access to all resources (users, orders, payments)

---

## Base URL & Headers

### Request Headers (All Endpoints)

```http
Authorization: Bearer {accessToken}
X-User-UUID: {userUuid}                    # Injected by gateway
X-User-Role: {role}                        # Injected by gateway
X-Correlation-ID: {requestId}              # Optional, for tracing
Content-Type: application/json
```

### Response Headers

```http
Content-Type: application/json
X-Correlation-ID: {same as request}        # For distributed tracing
Cache-Control: private, max-age=0           # Typical for dynamic content
```

---

## API Endpoints by Service

### 🔐 AUTH SERVICE - JWT Management

Base: `/api/auth`

#### 1. Login
```http
POST /api/auth/login
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 900000  // milliseconds (15 minutes)
}

Errors:
- 400 Bad Request: Invalid email/password format
- 401 Unauthorized: Email/password incorrect
- 400 Bad Request: Account not verified (emailVerified=false)
```

#### 2. Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

Request Body:
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "new-refresh-token-uuid",  // Old token revoked
  "tokenType": "Bearer",
  "expiresIn": 900000
}

Behavior:
- Old refresh token is revoked (cannot be used again)
- New refresh token issued
- Ensures token rotation (security best practice)

Errors:
- 400 Bad Request: Refresh token invalid/expired/revoked
```

#### 3. Logout
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}

Request Body:
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

Response (200 OK):
{}

Effect:
- Refresh token revoked
- Access token remains valid (expires naturally)
- User must login again to get new tokens
```

---

### 👤 USER SERVICE - Profile & Registration

Base: `/api/user`

#### 1. Register New User
```http
POST /api/user/register
Content-Type: application/json
Authorization: (not required)

Request Body:
{
  "email": "newuser@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-234-567-8900",
  "address": "123 Main St, NY 10001",
  "role": "BUYER"  // or "SELLER"
}

Response (201 Created):
{
  "uuid": "user-uuid-123",
  "email": "newuser@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "BUYER",
  "emailVerified": false,
  "status": "PENDING_EMAIL",  // Email verification needed
  "createdAt": "2026-02-15T10:00:00Z"
}

Next Steps:
- User receives OTP email
- Call POST /api/user/verify-otp with OTP code
- User status changes to ACTIVE

Errors:
- 400 Bad Request: Email already registered
- 400 Bad Request: Invalid password (< 8 chars)
- 400 Bad Request: Invalid email format
```

#### 2. Verify Email OTP
```http
POST /api/user/verify-otp
Content-Type: application/json
Authorization: (not required)

Request Body:
{
  "email": "newuser@example.com",
  "otp": "123456"  // 6-digit code from email
}

Response (200 OK):
{
  "verified": true,
  "message": "Email verified successfully"
}

Effect:
- User.emailVerified = true
- User.status = ACTIVE
- Can now login via POST /api/auth/login

Errors:
- 400 Bad Request: OTP invalid
- 400 Bad Request: OTP expired (valid 5 minutes)
- 404 Not Found: Email not found
```

#### 3. Get User Profile
```http
GET /api/user/{uuid}
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "user-uuid-123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-234-567-8900",
  "address": "123 Main St",
  "role": "BUYER",
  "emailVerified": true,
  "status": "ACTIVE",
  "createdAt": "2026-02-01T10:00:00Z",
  "updatedAt": "2026-02-15T10:00:00Z"
}

Authorization:
- Users can view their own profile
- ADMIN can view any profile
- Others: 403 Forbidden

If role = "SELLER", includes additional:
{
  "sellerDetail": {
    "uuid": "seller-detail-uuid",
    "businessName": "John's Electronics",
    "gstNumber": "18AABCT1234F1Z0",
    "bankAccounts": [...],
    "status": "APPROVED",  // PENDING, APPROVED, REJECTED
    "submittedAt": "2026-02-10T10:00:00Z",
    "approvedAt": "2026-02-14T10:00:00Z"
  }
}
```

#### 4. Update User Profile
```http
PUT /api/user/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "firstName": "Jane",
  "lastName": "Smith",
  "phone": "+1-987-654-3210",
  "address": "456 Oak Ave, CA 90210"
}

Response (200 OK):
{
  "uuid": "user-uuid-123",
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "phone": "+1-987-654-3210",
  "address": "456 Oak Ave, CA 90210",
  "role": "BUYER",
  "status": "ACTIVE",
  "updatedAt": "2026-02-15T11:00:00Z"
}

Authorization:
- Users can update their own profile only
- ADMIN can update any profile
```

#### 5. Submit Seller Verification (SELLER only)
```http
POST /api/user/seller-details
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "businessName": "John's Electronics Store",
  "gstNumber": "18AABCT1234F1Z0",
  "bankAccounts": [
    {
      "bankName": "HDFC Bank",
      "accountNumber": "1234567890123456",
      "ifscCode": "HDFC0001234",
      "accountHolderName": "John Doe"
    }
  ],
  "documents": {
    "businessRegistration": "url/to/document.pdf",
    "gstCertificate": "url/to/gst-cert.pdf",
    "bankProof": "url/to/bank-proof.pdf"
  }
}

Response (200 OK):
{
  "uuid": "seller-detail-uuid",
  "userId": "user-uuid-123",
  "businessName": "John's Electronics Store",
  "gstNumber": "18AABCT1234F1Z0",
  "status": "PENDING",
  "submittedAt": "2026-02-15T11:00:00Z"
}

Next Steps:
- ADMIN reviews documents
- Calls PUT /api/user/seller-details/{uuid} with APPROVED/REJECTED
- User notified via email
- If APPROVED: Can now create products
```

---

### 📦 PRODUCT SERVICE - Catalog Management

Base: `/api/product`

#### 1. Create Product (SELLER only)
```http
POST /api/product
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "Samsung Galaxy S24",
  "description": "Latest flagship smartphone with amazing camera",
  "price": 79999.99,
  "stock": 50,
  "category": "Electronics",
  "imageUrls": "url1.jpg;url2.jpg;url3.jpg"  // semicolon-separated
}

Response (201 Created):
{
  "uuid": "product-uuid-123",
  "sellerUuid": "seller-uuid",
  "name": "Samsung Galaxy S24",
  "description": "Latest flagship smartphone with amazing camera",
  "price": 79999.99,
  "stock": 50,
  "category": "Electronics",
  "imageUrls": ["url1.jpg", "url2.jpg", "url3.jpg"],
  "status": "ACTIVE",
  "rating": null,
  "reviewCount": 0,
  "createdAt": "2026-02-15T12:00:00Z"
}

Authorization:
- SELLER role required (@PreAuthorize)
- User must be verified (status = ACTIVE)
- User.sellerDetail.status must be APPROVED

Errors:
- 403 Forbidden: Not a seller / seller not approved
- 400 Bad Request: Price <= 0, stock < 0
```

#### 2. Get Product Details
```http
GET /api/product/{uuid}
Authorization: (optional, not required for public viewing)

Response (200 OK):
{
  "uuid": "product-uuid-123",
  "sellerUuid": "seller-uuid",
  "name": "Samsung Galaxy S24",
  "description": "Latest flagship smartphone with amazing camera",
  "price": 79999.99,
  "stock": 50,
  "category": "Electronics",
  "imageUrls": ["url1.jpg", "url2.jpg", "url3.jpg"],
  "status": "ACTIVE",
  "rating": 4.5,
  "reviewCount": 24,
  "createdAt": "2026-02-10T12:00:00Z"
}

Caching:
- Redis cached for 5 minutes
- Invalidated on update

Performance:
- Cache hit: ~10ms
- Database: ~50ms
```

#### 3. Search/List Products
```http
GET /api/product?page=0&size=20&category=Electronics&name=Samsung
Authorization: (optional)

Query Parameters:
- page: Zero-indexed page number (default 0)
- size: Results per page(default 20, max 100)
- category: Filter by category (optional)
- name: Search by product name (optional, partial match)
- minPrice: Minimum price (optional)
- maxPrice: Maximum price (optional)
- sortBy: createdAt, price, rating (default createdAt DESC)

Response (200 OK):
{
  "content": [
    {
      "uuid": "product-1",
      "name": "Samsung Galaxy S24",
      "price": 79999.99,
      "category": "Electronics",
      "imageUrls": ["url1.jpg"],
      "rating": 4.5,
      "reviewCount": 24
    },
    // ... more products
  ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3,
  "last": false
}

Performance:
- Database query with indexes on category, name, createdAt
- Typical: ~100-200ms for first page
```

#### 4. Update Product (SELLER only, own products)
```http
PUT /api/product/{uuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "Samsung Galaxy S24",
  "description": "Updated description",
  "price": 75999.99,  // Can adjust price
  "stock": 45,        // Can adjust stock
  "imageUrls": "new-url1.jpg;new-url2.jpg"
}

Response (200 OK):
{
  "uuid": "product-uuid-123",
  "name": "Samsung Galaxy S24",
  "price": 75999.99,
  "stock": 45,
  "updatedAt": "2026-02-15T13:00:00Z"
}

Authorization:
- SELLER role required
- Only own products can be edited
- Cannot update status (ACTIVE/INACTIVE via separate endpoint)
```

#### 5. Delete Product (SELLER only, soft delete)
```http
DELETE /api/product/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body, successful deletion)

Behavior:
- Soft delete (isDeleted = true)
- Product remains in database (audit trail)
- Removed from search results
- Existing orders with this product unaffected

Effect:
- Cannot place new orders with deleted product
- Existing orders still show product name/price
```

---

### 🛒 ORDER SERVICE - Order Processing

Base: `/api/order`

#### 1. Create Order (BUYER only) - WITH SAGA PATTERN
```http
POST /api/order
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "items": [
    {"productUuid": "product-1", "quantity": 2},
    {"productUuid": "product-2", "quantity": 1}
  ],
  "shippingName": "John Doe",
  "shippingAddress": "123 Main St",
  "shippingCity": "New York",
  "shippingState": "NY",
  "shippingPincode": "10001",
  "shippingPhone": "+1-234-567-8900"
}

Response (200 OK):
{
  "uuid": "order-uuid-123",
  "buyerUuid": "buyer-uuid",
  "totalAmount": 2500.00,
  "status": "CREATED",
  "paymentStatus": "PENDING",  // ← Saga in progress
  "items": [
    {
      "productUuid": "product-1",
      "sellerUuid": "seller-1",
      "quantity": 2,
      "price": 1000.00
    }
  ],
  "shipping": {
    "name": "John Doe",
    "address": "123 Main St",
    "city": "New York",
    "state": "NY",
    "pincode": "10001",
    "phone": "+1-234-567-8900"
  },
  "createdAt": "2026-02-15T14:00:00Z"
}

SAGA FLOW (Behind the Scenes):
1. ✅ Stock reduced synchronously (OrderServiceImpl.createOrder)
2. 📨 order.created Kafka event published
3. ⏳ payment-service consumes event
4. ⏳ payment-service processes payment
5. ⏳ payment.completed Kafka event published
6. ⏳ order-service updates paymentStatus
7. 🔄 Client polls GET /api/order/{uuid} to check status

Client Polling Pattern:
Loop:
  status = GET /api/order/{uuid}
  if status.paymentStatus == "PENDING"
    sleep 2 seconds, continue
  else if status.paymentStatus == "SUCCESS"
    show confirmation, stop
  else if status.paymentStatus == "FAILED"
    show error, allow retry

Authorization:
- BUYER role required
- User is from JWT token (buyerUuid)

Errors:
- 400 Bad Request: Items empty, product not found
- 400 Bad Request: Insufficient stock
- 400 Bad Request: Product not ACTIVE
- 503 Service Unavailable: product-service down (compensation: restore stock)
```

#### 2. Get Order Details
```http
GET /api/order/{uuid}
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "order-uuid-123",
  "buyerUuid": "buyer-uuid",
  "totalAmount": 2500.00,
  "status": "CREATED",
  "paymentStatus": "SUCCESS",  // Updated via saga
  "items": [...],
  "shipping": {...},
  "createdAt": "2026-02-15T14:00:00Z",
  "updatedAt": "2026-02-15T14:05:00Z"  // When payment completed
}

Authorization:
- BUYER: Can see own orders only
- SELLER: Can see orders with their items
- ADMIN: Can see any order

Caching:
- Redis cached (first access or after cache eviction)
- Invalidated on status updates
- TTL: 5 minutes typically

Typical Use:
- Client polls every 2 seconds while paymentStatus = PENDING
- Stops when status changes (SUCCESS or FAILED)
```

#### 3. List Orders
```http
GET /api/order?page=0&size=10
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page (default 0)
- size: Per page (default 10, max 50)

Response (200 OK):
{
  "content": [
    {
      "uuid": "order-1",
      "totalAmount": 2500.00,
      "status": "CREATED",
      "paymentStatus": "SUCCESS",
      "createdAt": "2026-02-15T14:00:00Z"
    },
    // ... more orders
  ],
  "page": 0,
  "size": 10,
  "totalElements": 47,
  "totalPages": 5,
  "last": false
}

Authorization:
- BUYER: Returns only their orders
- ADMIN: Returns all orders
- SELLER: Cannot access (use /api/order/seller instead)

Sorting:
- Default: createdAt DESC (newest first)
```

#### 4. List Seller's Orders
```http
GET /api/order/seller?page=0&size=10
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "content": [
    {
      "uuid": "order-1",
      "buyerUuid": "buyer-uuid",
      "items": [
        {
          "productUuid": "product-uuid",
          "quantity": 2,
          "sellerUuid": "your-seller-uuid"
        }
      ],
      "totalAmount": 2500.00,
      "status": "CREATED",
      "paymentStatus": "SUCCESS",
      "createdAt": "2026-02-15T14:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "last": false
}

Authorization:
- SELLER role required
- Returns only orders containing seller's items

Use Case:
- Seller dashboard → View orders to fulfill
- Check which items to pack, ship
```

#### 5. Update Order Status
```http
PUT /api/order/{uuid}/status?status=SHIPPED
Authorization: Bearer {accessToken}

Query Parameter:
- status: Next status (CONFIRMED, SHIPPED, DELIVERED, or CANCELLED)

Response (200 OK):
{
  "uuid": "order-uuid-123",
  "status": "SHIPPED",
  "updatedAt": "2026-02-15T15:00:00Z"
}

State Transitions (Enforced):
Created → Confirmed → Shipped → Delivered

By Role:
- ADMIN: Can transition any order forward
- SELLER: Can transition orders with their items forward
- BUYER: Can only cancel (if status = CREATED)

Cancellation:
- BUYER calls: PUT /api/order/{uuid}/status?status=CANCELLED
- Only allowed if status = CREATED (before payment processed)
- Compensation: Stock restored for all items
- paymentStatus remains PENDING (no refund processing yet)
```

---

### 💰 PAYMENT SERVICE - Payment Processing

Base: `/api/payment`

#### 1. Payment History (BUYER)
```http
GET /api/payment?page=0&size=10
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "content": [
    {
      "uuid": "payment-uuid-1",
      "orderUuid": "order-uuid-1",
      "amount": 2500.00,
      "status": "SUCCESS",
      "createdAt": "2026-02-15T14:00:00Z"
    },
    {
      "uuid": "payment-uuid-2",
      "orderUuid": "order-uuid-2",
      "amount": 1200.00,
      "status": "FAILED",
      "createdAt": "2026-02-14T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 24,
  "totalPages": 3,
  "last": false
}

Authorization:
- BUYER: See own payments
- ADMIN: See all payments

Filtering:
- Can add ?status=SUCCESS or ?status=FAILED
```

#### 2. Seller Payment Dashboard
```http
GET /api/seller/dashboard
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "totalEarnings": 50000.00,
  "pendingBalance": 5000.00,
  "availableBalance": 45000.00,
  "settledCount": 120,
  "pendingCount": 5,
  "splits": [
    {
      "uuid": "split-uuid-1",
      "orderUuid": "order-1",
      "amount": 900.00,  // (orderAmount - commissions - fees) * seller_ratio
      "status": "PAID",
      "paidAt": "2026-02-14T10:00:00Z"
    },
    {
      "uuid": "split-uuid-2",
      "orderUuid": "order-2",
      "amount": 850.00,
      "status": "PENDING",
      "createdAt": "2026-02-15T14:00:00Z"
    }
  ]
}

Authorization:
- SELLER role required
- Shows own earnings for all orders seller has items in

Calculation Example (for $2000 order with 1 seller, 3 items):
  Subtotal: $2000.00
  Platform Commission (10%): -$200.00
  Delivery Fees (₹30/$1.5 per item): -$4.50
  Seller Receives: $1795.50
  (Assumes single seller; multi-seller splits proportionally)
```

#### 3. Admin Finance Dashboard
```http
GET /api/admin/dashboard
Authorization: Bearer {accessToken}
(ADMIN role required)

Response (200 OK):
{
  "totalRevenue": 500000.00,
  "totalOrders": 250,
  "totalPayments": 248,
  "failedPayments": 2,
  "platformCommission": 50000.00,
  "deliveryFeeRevenue": 3750.00,
  "pendingPayouts": 25000.00,
  "paymentBreakdown": [
    {"status": "SUCCESS", "count": 248, "amount": 495000.00},
    {"status": "FAILED", "count": 2, "amount": 5000.00},
    {"status": "PENDING", "count": 0, "amount": 0}
  ]
}

Authorization:
- ADMIN role only

Use Case:
- Finance team reviews platform metrics
- Calculate revenue, commission, payouts
- Track payment failures
```

---

### ⭐ REVIEW SERVICE - Product Reviews

Base: `/api/review`

#### 1. Create Review (BUYER only, post-delivery)
```http
POST /api/review
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "orderUuid": "order-uuid-123",
  "productUuid": "product-uuid-1",
  "rating": 4,                    // 1-5 stars
  "comment": "Good product, delivery was fast!"
}

Response (201 Created):
{
  "uuid": "review-uuid-1",
  "orderUuid": "order-uuid-123",
  "productUuid": "product-uuid-1",
  "sellerUuid": "seller-uuid",
  "buyerUuid": "buyer-uuid",
  "rating": 4,
  "comment": "Good product, delivery was fast!",
  "createdAt": "2026-02-15T16:00:00Z"
}

Constraints:
- Order must exist in system
- Order must be DELIVERED (verified via Feign call to order-service)
- Buyer (JWT uuid) must match order.buyerUuid
- Product must be in order.items
- One review per buyer per product (duplicate blocked)

Errors:
- 403 Forbidden: Not order buyer
- 400 Bad Request: Order not DELIVERED
- 400 Bad Request: Rating not 1-5
- 400 Bad Request: Already reviewed product (update via PUT instead)
- 404 Not Found: Order/product not found

Side Effects:
- Kafka event: review.submitted published
- Product cache invalidated
- Product review aggregate updated (count, average rating)
```

#### 2. Get Product Reviews
```http
GET /api/review/product/{productUuid}?page=0&size=10
Authorization: (optional)

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 10, max 50)
- minRating: Filter by minimum rating (1-5)
- maxRating: Filter by maximum rating

Response (200 OK):
{
  "content": [
    {
      "uuid": "review-1",
      "buyerName": "John D.",        // Anonymized
      "rating": 5,
      "comment": "Excellent product!",
      "createdAt": "2026-02-14T10:00:00Z"
    },
    {
      "uuid": "review-2",
      "buyerName": "Jane S.",
      "rating": 4,
      "comment": "Good, but packaging could be better",
      "createdAt": "2026-02-13T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 24,
  "totalPages": 3,
  "last": false,
  "aggregates": {
    "averageRating": 4.5,
    "totalReviews": 24,
    "distribution": {
      "5": 12,
      "4": 8,
      "3": 3,
      "2": 1,
      "1": 0
    }
  }
}

Sorting:
- Default: createdAt DESC (newest first)
- Can sort by: rating DESC, helpful count DESC

Caching:
- Redis cached (product {uuid}:reviews)
- Invalidated on new review
```

#### 3. My Reviews (BUYER)
```http
GET /api/review/my-reviews?page=0&size=10
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "content": [
    {
      "uuid": "review-1",
      "productUuid": "product-1",
      "productName": "Samsung Galaxy S24",
      "rating": 5,
      "comment": "Excellent product!",
      "createdAt": "2026-02-14T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}

Authorization:
- BUYER role required
- Returns only reviews by logged-in user
```

#### 4. Update Review (BUYER only, own reviews)
```http
PUT /api/review/{uuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "rating": 3,  // Changed from 4
  "comment": "Actually, the product is not as good as expected"
}

Response (200 OK):
{
  "uuid": "review-uuid-1",
  "rating": 3,
  "comment": "Actually, the product is not as good as expected",
  "updatedAt": "2026-02-15T17:00:00Z"
}

Authorization:
- BUYER role required
- Can only update own reviews
- Must be review author (checked via buyerUuid)

Side Effects:
- Product cache invalidated
- Review aggregate updated (recalculate average rating)
```

#### 5. Delete Review (BUYER only)
```http
DELETE /api/review/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted = true)
- Review remains in database (audit trail)
- Not shown in review lists
- Product aggregate recalculated

Authorization:
- BUYER role required
- Own reviews only
```

---

## Request/Response Examples

### Complete Order Workflow

#### Step 1: Register & Login
```bash
# Register
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "buyer@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "role": "BUYER"
  }'

# Verify OTP (check email)
curl -X POST http://localhost:8080/api/user/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "buyer@example.com",
    "otp": "123456"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "buyer@example.com",
    "password": "SecurePass123!"
  }' > login_response.json

# Extract tokens
accessToken=$(jq -r '.accessToken' login_response.json)
userUuid=$(curl -s http://localhost:8080/api/user/{userUuid} \
  -H "Authorization: Bearer $accessToken" | jq -r '.uuid')
```

#### Step 2: Create Order (Saga Triggered)
```bash
# POST /api/order
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $accessToken" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productUuid": "prod-1", "quantity": 2}
    ],
    "shippingName": "John Doe",
    "shippingAddress": "123 Main St",
    "shippingCity": "New York",
    "shippingState": "NY",
    "shippingPincode": "10001",
    "shippingPhone": "+1-234-567-8900"
  }' > order_response.json

orderUuid=$(jq -r '.uuid' order_response.json)
echo "Order created: $orderUuid"
echo "Payment status: $(jq -r '.paymentStatus' order_response.json)"  # PENDING
```

#### Step 3: Poll for Payment Completion
```bash
# Client polls every 2 seconds
for i in {1..30}; do
  status=$(curl -s http://localhost:8080/api/order/$orderUuid \
    -H "Authorization: Bearer $accessToken" | jq -r '.paymentStatus')
  
  echo "Poll #$i: Payment status = $status"
  
  if [ "$status" != "PENDING" ]; then
    echo "Payment processing complete!"
    break
  fi
  
  sleep 2
done
```

#### Step 4: Leave Review (After Delivery)
```bash
# Wait for order.status = DELIVERED (admin updates)
# Then create review
curl -X POST http://localhost:8080/api/review \
  -H "Authorization: Bearer $accessToken" \
  -H "Content-Type: application/json" \
  -d '{
    "orderUuid": "'$orderUuid'",
    "productUuid": "prod-1",
    "rating": 5,
    "comment": "Great product! Excellent service."
  }'
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "timestamp": "2026-02-15T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Product not found: invalid-uuid",
  "path": "/api/order"
}
```

### HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| **200** | Success | GET /api/order returned order details |
| **201** | Created | POST /api/product returned new product |
| **204** | No Content | DELETE /api/review successful |
| **400** | Bad Request | Invalid JSON, validation error |
| **401** | Unauthorized | JWT missing/invalid/expired |
| **403** | Forbidden | Role insufficient (not BUYER for POST /order) |
| **404** | Not Found | Resource UUID doesn't exist |
| **409** | Conflict | Business rule violation (duplicate review) |
| **500** | Server Error | Unexpected exception |
| **503** | Service Unavailable | Downstream service down (product-service) |

### Common Exceptions

#### OrderAccessException (403)
```json
{
  "status": 403,
  "message": "You can only view your own orders"
}
```

#### OrderNotFoundException (404)
```json
{
  "status": 404,
  "message": "Order not found: invalid-uuid"
}
```

#### OrderStateException (400)
```json
{
  "status": 400,
  "message": "Product is not active: product-uuid"
}
```

#### ValidationException (400)
```json
{
  "status": 400,
  "error": "Validation failed",
  "details": [
    {
      "field": "items",
      "message": "must not be empty"
    }
  ]
}
```

---

## Workflow Examples

### Example 1: Complete Buyer Journey

```
1. Buyer navigates to platform (frontend)
2. Clicks "Sign Up"
   → POST /api/user/register
   → Receives OTP email

3. Enters OTP from email
   → POST /api/user/verify-otp
   → Account activated

4. Logs in
   → POST /api/auth/login
   → Receives JWT tokens

5. Browses products
   → GET /api/product?page=0&size=20
   → GET /api/product/{uuid} for details
   → GET /api/review/product/{uuid} checks reviews

6. Places order
   → POST /api/order {items, shipping}
   → Order created (status=CREATED, paymentStatus=PENDING)
   → [Saga triggered in background]

7. Waits for payment
   → Client polls GET /api/order/{uuid} every 2 seconds
   → Watches paymentStatus: PENDING → SUCCESS

8. Order confirmed
   → Frontend shows confirmation
   → Order enters fulfillment (CREATED → CONFIRMED → SHIPPED)

9. Receives order
   → Order status becomes DELIVERED

10. Leaves review
    → POST /api/review {rating, comment}
    → Review published

11. Views order details
    → GET /api/order/{uuid}
    → GET /api/review/my-reviews
```

### Example 2: Seller Story

```
1. Seller registers with role=SELLER
   → POST /api/user/register {role: "SELLER"}

2. Submits verification documents
   → POST /api/user/seller-details {business info, docs}
   → Status = PENDING (awaiting admin approval)

3. Admin approves
   → PUT /api/user/seller-details/{uuid} {status: "APPROVED"}
   → Email notification sent

4. Seller creates products
   → POST /api/product {name, price, stock, images}
   → Products listed on platform

5. Customer orders seller's product
   → POST /api/order {items: [seller's product]}

6. Seller views orders to fulfill
   → GET /api/order/seller

7. Seller ships order
   → PUT /api/order/{uuid}/status?status=SHIPPED
   → Order status transitions

8. Seller checks earnings
   → GET /api/seller/dashboard
   → Sees available balance, pending payouts

9. Customer leaves review
   → Product rating updated

10. Seller views review
    → Reviews visible in product page
```

---

## Testing the API

### Using cURL

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@test.com", "password": "password"}' \
  | jq -r '.accessToken')

# 2. Create order
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{...}' |jq

# 3. Get order
curl -X GET http://localhost:8080/api/order/order-uuid \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Using Postman

1. Create collection "E-Commerce Backend"
2. Add requests for each endpoint
3. Use variables: `{{accessToken}}`, `{{userUuid}}`
4. Set pre-request script to handle login
5. Test entire workflows

### Using Swagger/OpenAPI

```bash
# Access Swagger UI at:
http://localhost:8080/swagger-ui.html

# API docs JSON:
http://localhost:8080/v3/api-docs
```

---

**Last Updated**: February 2026  
**Version**: 2.0 - Complete API Documentation Bundle
