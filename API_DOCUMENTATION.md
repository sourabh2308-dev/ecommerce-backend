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

#### 6. Forgot Password
```http
POST /api/user/forgot-password
Content-Type: application/json
Authorization: (not required)

Request Body:
{
  "email": "user@example.com"
}

Response (200 OK):
{
  "message": "Password reset link sent to your email",
  "email": "user@example.com"
}

Behavior:
- Email sent with password reset token (valid 30 minutes)
- User clicks link in email
- Frontend redirects to reset password page with token
- User enters new password
- Call POST /api/user/reset-password with token

Errors:
- 404 Not Found: Email not registered
```

#### 7. Reset Password
```http
POST /api/user/reset-password
Content-Type: application/json
Authorization: (not required)

Request Body:
{
  "token": "reset-token-from-email-link",
  "newPassword": "NewSecurePassword123!"
}

Response (200 OK):
{
  "message": "Password reset successfully",
  "email": "user@example.com"
}

Validation:
- Token must be valid and not expired (30 minutes)
- New password must meet requirements (min 8 chars, uppercase, lowercase, digit, special char)
- Cannot reuse last 3 passwords
- All existing sessions invalidated (user must login again)

Errors:
- 400 Bad Request: Token invalid/expired
- 400 Bad Request: Password doesn't meet requirements
- 404 Not Found: Email not found
```

#### 8. Create Address
```http
POST /api/user/addresses
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "label": "Home",              // Home, Work, Other
  "name": "John Doe",
  "phone": "+1-234-567-8900",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "pincode": "10001",
  "isDefault": false
}

Response (201 Created):
{
  "uuid": "address-uuid-1",
  "userUuid": "user-uuid-123",
  "label": "Home",
  "name": "John Doe",
  "phone": "+1-234-567-8900",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "pincode": "10001",
  "isDefault": false,
  "createdAt": "2026-02-15T12:00:00Z"
}

Constraints:
- Max 5 addresses per user
- Only one default address allowed

Authorization:
- BUYER role
- Can only add addresses to their own account

Errors:
- 400 Bad Request: Max addresses exceeded (5)
- 400 Bad Request: Invalid phone/pincode format
```

#### 9. Update Address
```http
PUT /api/user/addresses/{uuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "label": "Home",
  "name": "Jane Doe",
  "phone": "+1-987-654-3210",
  "address": "456 Oak Avenue",
  "city": "Los Angeles",
  "state": "CA",
  "pincode": "90210",
  "isDefault": true
}

Response (200 OK):
{
  "uuid": "address-uuid-1",
  "label": "Home",
  "name": "Jane Doe",
  "phone": "+1-987-654-3210",
  "address": "456 Oak Avenue",
  "city": "Los Angeles",
  "state": "CA",
  "pincode": "90210",
  "isDefault": true,
  "updatedAt": "2026-02-15T13:00:00Z"
}

Side Effects:
- If isDefault=true, previous default address set to false

Authorization:
- BUYER role, own addresses only
```

#### 10. Delete Address
```http
DELETE /api/user/addresses/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted=true)
- If was default, next address becomes default
- Can still appear in order history (audit trail)

Errors:
- 404 Not Found: Address not found
- 403 Forbidden: Not your address
```

#### 11. List Addresses
```http
GET /api/user/addresses
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "addresses": [
    {
      "uuid": "address-uuid-1",
      "label": "Home",
      "name": "John Doe",
      "address": "123 Main Street",
      "city": "New York",
      "state": "NY",
      "pincode": "10001",
      "isDefault": true,
      "createdAt": "2026-02-15T12:00:00Z"
    },
    {
      "uuid": "address-uuid-2",
      "label": "Work",
      "name": "John Doe",
      "address": "789 Broadway",
      "city": "New York",
      "state": "NY",
      "pincode": "10002",
      "isDefault": false,
      "createdAt": "2026-02-14T10:00:00Z"
    }
  ]
}

Authorization:
- BUYER role, own addresses only
```

#### 12. Set Default Address
```http
PUT /api/user/addresses/{uuid}/set-default
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "address-uuid-1",
  "isDefault": true,
  "updatedAt": "2026-02-15T13:30:00Z"
}

Side Effects:
- Previous default address set to isDefault=false
- Only one default per user maintained

Errors:
- 404 Not Found: Address not found
- 403 Forbidden: Not your address
```

#### 13. Add Item to Shopping Cart
```http
POST /api/user/cart/items
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "productUuid": "product-uuid-1",
  "quantity": 2,
  "variantId": "variant-id-1"  // Optional, for variants
}

Response (201 Created):
{
  "uuid": "cart-item-uuid-1",
  "cartUuid": "cart-uuid",
  "productUuid": "product-uuid-1",
  "productName": "Samsung Galaxy S24",
  "quantity": 2,
  "price": 79999.99,
  "subtotal": 159999.98,
  "variantId": "variant-id-1",
  "addedAt": "2026-02-15T14:00:00Z"
}

Behavior:
- Cart auto-created if doesn't exist
- If product already in cart, quantity updated (PUT instead)
- Stock validation: quantity <= available stock
- Price fetched at add-time (product price updates tracked)

Errors:
- 404 Not Found: Product not found
- 400 Bad Request: Product not ACTIVE
- 400 Bad Request: Quantity > available stock
- 403 Forbidden: Invalid variant for product
```

#### 14. Update Cart Item
```http
PUT /api/user/cart/items/{cartItemUuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "quantity": 3
}

Response (200 OK):
{
  "uuid": "cart-item-uuid-1",
  "productUuid": "product-uuid-1",
  "quantity": 3,
  "price": 79999.99,
  "subtotal": 239999.97,
  "updatedAt": "2026-02-15T14:05:00Z"
}

Validation:
- Quantity must be > 0
- Quantity must not exceed stock
- Stock checked against current inventory

Errors:
- 400 Bad Request: Quantity invalid
- 400 Bad Request: Insufficient stock
```

#### 15. Remove Item from Cart
```http
DELETE /api/user/cart/items/{cartItemUuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Item removed from cart
- Cart persists even if empty

Errors:
- 404 Not Found: Cart item not found
```

#### 16. Clear Shopping Cart
```http
DELETE /api/user/cart
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- All items removed
- Cart object persists (empty)

Effect:
- User can immediately add new items
```

#### 17. Get Shopping Cart
```http
GET /api/user/cart
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "cart-uuid",
  "items": [
    {
      "uuid": "cart-item-uuid-1",
      "productUuid": "product-uuid-1",
      "productName": "Samsung Galaxy S24",
      "quantity": 2,
      "price": 79999.99,
      "subtotal": 159999.98,
      "sellerUuid": "seller-uuid-1",
      "variantId": "variant-1"
    },
    {
      "uuid": "cart-item-uuid-2",
      "productUuid": "product-uuid-2",
      "productName": "iPhone 16 Pro",
      "quantity": 1,
      "price": 119999.99,
      "subtotal": 119999.99,
      "sellerUuid": "seller-uuid-2",
      "variantId": null
    }
  ],
  "totals": {
    "itemCount": 3,
    "subtotal": 279999.97,
    "estimatedShipping": 150.00,
    "estimatedTax": 29200.00,
    "total": 309349.97
  },
  "updatedAt": "2026-02-15T14:00:00Z"
}

Features:
- Real-time stock validation
- Current prices fetched
- Shipping estimate based on items
- Tax calculation (varies by items)
- Groups by seller for splitting orders

Caching:
- Not cached (always fresh data)
```

#### 18. Add to Wishlist
```http
POST /api/user/wishlist
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "productUuid": "product-uuid-1"
}

Response (201 Created):
{
  "uuid": "wishlist-item-uuid-1",
  "productUuid": "product-uuid-1",
  "productName": "Samsung Galaxy S24",
  "price": 79999.99,
  "addedAt": "2026-02-15T14:00:00Z"
}

Behavior:
- Creates personal wishlist if doesn't exist
- Duplicate prevented (cannot add same product twice)
- Wishlist is user-private

Errors:
- 404 Not Found: Product not found
- 409 Conflict: Product already in wishlist
```

#### 19. Remove from Wishlist
```http
DELETE /api/user/wishlist/{wishlistItemUuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Errors:
- 404 Not Found: Wishlist item not found
```

#### 20. Get Wishlist
```http
GET /api/user/wishlist?page=0&size=20
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page (default 0)
- size: Items per page (default 20, max 100)

Response (200 OK):
{
  "content": [
    {
      "uuid": "wishlist-item-uuid-1",
      "productUuid": "product-uuid-1",
      "productName": "Samsung Galaxy S24",
      "price": 79999.99,
      "currentPrice": 75999.99,      // Price may have changed
      "priceDropped": true,
      "rating": 4.5,
      "imageUrl": "image1.jpg",
      "addedAt": "2026-02-15T14:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}

Features:
- Shows current product prices
- Flags price drops
- Shows product ratings
- Favorite items easily moved to cart
```

#### 21. List Notifications
```http
GET /api/user/notifications?page=0&size=20&unreadOnly=false
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page (default 0)
- size: Per page (default 20, max 100)
- unreadOnly: Show only unread (optional, default false)

Response (200 OK):
{
  "content": [
    {
      "uuid": "notification-uuid-1",
      "type": "ORDER_CONFIRMED",     // ORDER_*, REVIEW_*, PAYMENT_*, etc.
      "title": "Order Confirmed",
      "message": "Your order #ORD-001 has been confirmed",
      "entityUuid": "order-uuid-1",
      "isRead": false,
      "createdAt": "2026-02-15T14:30:00Z"
    },
    {
      "uuid": "notification-uuid-2",
      "type": "PAYMENT_SUCCESSFUL",
      "title": "Payment Successful",
      "message": "Payment of $2500 received for order #ORD-001",
      "entityUuid": "order-uuid-1",
      "isRead": true,
      "createdAt": "2026-02-15T14:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3,
  "last": false,
  "unreadCount": 12
}

Notification Types:
- ORDER_CREATED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED
- PAYMENT_SUCCESSFUL, PAYMENT_FAILED, REFUND_ISSUED
- REVIEW_RECEIVED, REVIEW_HELPFUL_VOTED
- SELLER_APPROVED, PRODUCT_ACTIVATED
- WISHLIST_PRICE_DROP
```

#### 22. Mark Notification as Read
```http
PUT /api/user/notifications/{uuid}/read
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "notification-uuid-1",
  "isRead": true,
  "updatedAt": "2026-02-15T15:00:00Z"
}
```

#### 23. Delete Notification
```http
DELETE /api/user/notifications/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted=true)
- Remains in database for audit trail
```

#### 24. Get Loyalty Points
```http
GET /api/user/loyalty/points
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "totalPoints": 5250,
  "availablePoints": 5000,
  "pendingPoints": 250,          // Points from recent orders, pending confirmation
  "memberTier": "GOLD",          // SILVER, GOLD, PLATINUM
  "nextTierThreshold": 10000,
  "pointsToNextTier": 4750,
  "lastUpdated": "2026-02-15T14:00:00Z"
}

Member Tiers:
- SILVER: 0-5000 points
- GOLD: 5001-15000 points (earn 1.5x points)
- PLATINUM: 15001+ points (earn 2x points)

Point Earning Rules:
- Every $1 spent = 1 point
- GOLD tier: 1 point per $0.67 spent (1.5x multiplier)
- PLATINUM tier: 1 point per $0.50 spent (2x multiplier)
- Birthday month: +500 bonus points
- First review: +50 points
```

#### 25. Redeem Loyalty Points
```http
POST /api/user/loyalty/redeem
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "points": 1000,              // Points to redeem
  "redemptionType": "DISCOUNT" // DISCOUNT or GIFT_CARD
}

Response (201 Created):
{
  "uuid": "redemption-uuid-1",
  "pointsRedeemed": 1000,
  "discountValue": 500.00,      // 100 points = $50 discount
  "redemptionType": "DISCOUNT",
  "status": "SUCCESS",
  "redemptionCode": "LOYALTY-ABC123",  // For DISCOUNT type
  "redeemedAt": "2026-02-15T15:00:00Z"
}

Redemption Rates:
- 100 points = $50 discount
- 250 points = $150 discount
- Can redeem in multiples of 100

Constraints:
- Cannot redeem pending points
- Minimum 100 points required
- Cannot exceed available points

Errors:
- 400 Bad Request: Insufficient points
- 400 Bad Request: Invalid redemption type
- 400 Bad Request: Points not multiple of 100
```

#### 26. Loyalty Transaction History
```http
GET /api/user/loyalty/transactions?page=0&size=10
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 10, max 50)
- type: EARNED, REDEEMED, EXPIRED (optional)

Response (200 OK):
{
  "content": [
    {
      "uuid": "transaction-uuid-1",
      "type": "EARNED",
      "points": 250,
      "reason": "Order #ORD-001 delivered",
      "referenceUuid": "order-uuid-1",
      "expiryDate": "2027-02-15T00:00:00Z",
      "createdAt": "2026-02-15T14:00:00Z"
    },
    {
      "uuid": "transaction-uuid-2",
      "type": "REDEEMED",
      "points": -1000,
      "reason": "Redeemed for discount code",
      "redemptionCode": "LOYALTY-ABC123",
      "createdAt": "2026-02-14T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 24,
  "totalPages": 3,
  "last": false
}

Point Expiry:
- Points expire after 2 years of inactivity
- Earned points show expiryDate
```

#### 27. Create Support Ticket
```http
POST /api/user/support-tickets
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "category": "PRODUCT_ISSUE",  // PRODUCT_ISSUE, DELIVERY, PAYMENT, ACCOUNT, OTHER
  "subject": "Product arrived damaged",
  "description": "The phone arrived with a broken screen",
  "orderUuid": "order-uuid-1",  // Optional, if related to order
  "priority": "HIGH"             // LOW, MEDIUM, HIGH
}

Response (201 Created):
{
  "uuid": "ticket-uuid-1",
  "ticketNumber": "TKT-2026-0001234",
  "category": "PRODUCT_ISSUE",
  "subject": "Product arrived damaged",
  "description": "The phone arrived with a broken screen",
  "status": "OPEN",               // OPEN, IN_PROGRESS, RESOLVED, CLOSED
  "priority": "HIGH",
  "assignedTo": null,             // Support agent assigned later
  "createdAt": "2026-02-15T15:30:00Z"
}

SLA (Service Level Agreement):
- HIGH: Response within 2 hours
- MEDIUM: Response within 24 hours
- LOW: Response within 48 hours

Errors:
- 400 Bad Request: Invalid category
- 404 Not Found: Order not found (if specified)
```

#### 28. List Support Tickets
```http
GET /api/user/support-tickets?page=0&size=10&status=OPEN
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 10, max 50)
- status: OPEN, IN_PROGRESS, RESOLVED, CLOSED (optional)
- category: Filter by category (optional)

Response (200 OK):
{
  "content": [
    {
      "uuid": "ticket-uuid-1",
      "ticketNumber": "TKT-2026-0001234",
      "category": "PRODUCT_ISSUE",
      "subject": "Product arrived damaged",
      "status": "OPEN",
      "priority": "HIGH",
      "lastReplyAt": "2026-02-15T16:00:00Z",
      "createdAt": "2026-02-15T15:30:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}

Authorization:
- BUYER: See own tickets only
- ADMIN/SUPPORT: See all tickets
```

#### 29. Get Ticket Details
```http
GET /api/user/support-tickets/{uuid}
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "ticket-uuid-1",
  "ticketNumber": "TKT-2026-0001234",
  "category": "PRODUCT_ISSUE",
  "subject": "Product arrived damaged",
  "description": "The phone arrived with a broken screen",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "assignedTo": {
    "uuid": "support-agent-uuid",
    "name": "Agent Name",
    "email": "agent@support.com"
  },
  "comments": [
    {
      "uuid": "comment-uuid-1",
      "author": "Support Agent",
      "message": "We sincerely apologize. We will issue a replacement immediately.",
      "createdAt": "2026-02-15T16:00:00Z"
    },
    {
      "uuid": "comment-uuid-2",
      "author": "John Doe",
      "message": "Thank you for the quick response!",
      "createdAt": "2026-02-15T16:30:00Z"
    }
  ],
  "createdAt": "2026-02-15T15:30:00Z",
  "updatedAt": "2026-02-15T16:30:00Z"
}
```

#### 30. Add Comment to Ticket
```http
POST /api/user/support-tickets/{uuid}/comments
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "message": "Following up on the issue. Has the replacement been shipped?"
}

Response (201 Created):
{
  "uuid": "comment-uuid-3",
  "ticketUuid": "ticket-uuid-1",
  "author": "John Doe",
  "message": "Following up on the issue. Has the replacement been shipped?",
  "createdAt": "2026-02-15T17:00:00Z"
}

Behavior:
- Auto-updates ticket.updatedAt and ticket.lastReplyAt
- If support agent replied, notification sent to buyer
- If buyer replied, notification sent to assigned agent

Errors:
- 404 Not Found: Ticket not found
- 403 Forbidden: Cannot comment on ticket (not author/agent)
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

#### 6. Create Category (ADMIN only)
```http
POST /api/product/categories
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "Electronics",
  "slug": "electronics",
  "description": "Electronic devices and gadgets",
  "imageUrl": "category-image.jpg",
  "parentCategoryUuid": null  // Optional, for nested categories
}

Response (201 Created):
{
  "uuid": "category-uuid-1",
  "name": "Electronics",
  "slug": "electronics",
  "description": "Electronic devices and gadgets",
  "imageUrl": "category-image.jpg",
  "parentCategoryUuid": null,
  "status": "ACTIVE",
  "createdAt": "2026-02-15T12:00:00Z"
}

Authorization:
- ADMIN role required

Constraints:
- Name must be unique
- Slug must be unique and URL-friendly
```

#### 7. Update Category (ADMIN only)
```http
PUT /api/product/categories/{uuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "Electronics & Gadgets",
  "description": "Electronic devices and smart gadgets",
  "imageUrl": "updated-category-image.jpg"
}

Response (200 OK):
{
  "uuid": "category-uuid-1",
  "name": "Electronics & Gadgets",
  "slug": "electronics",
  "description": "Electronic devices and smart gadgets",
  "imageUrl": "updated-category-image.jpg",
  "status": "ACTIVE",
  "updatedAt": "2026-02-15T13:00:00Z"
}

Authorization:
- ADMIN role required
```

#### 8. Delete Category (ADMIN only)
```http
DELETE /api/product/categories/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted=true)
- Products in category not affected
- Category hidden from category listing

Errors:
- 400 Bad Request: Category has nested subcategories
- 404 Not Found: Category not found
```

#### 9. List Categories
```http
GET /api/product/categories?includeInactive=false
Authorization: (optional)

Query Parameters:
- includeInactive: Show deleted categories (default false)
- parentOnly: Show only top-level categories (optional)

Response (200 OK):
{
  "categories": [
    {
      "uuid": "category-uuid-1",
      "name": "Electronics",
      "slug": "electronics",
      "description": "Electronic devices and gadgets",
      "imageUrl": "category-image.jpg",
      "productCount": 245,
      "subcategories": [
        {
          "uuid": "category-uuid-2",
          "name": "Smartphones",
          "slug": "smartphones",
          "productCount": 120
        }
      ]
    }
  ]
}

Features:
- Hierarchical category structure
- Product count per category
- Redis cached (5-minute TTL)
```

#### 10. Get Category by ID
```http
GET /api/product/categories/{uuid}
Authorization: (optional)

Response (200 OK):
{
  "uuid": "category-uuid-1",
  "name": "Electronics",
  "slug": "electronics",
  "description": "Electronic devices and gadgets",
  "imageUrl": "category-image.jpg",
  "parentCategoryUuid": null,
  "subcategoryCount": 5,
  "productCount": 245,
  "status": "ACTIVE",
  "createdAt": "2026-02-15T12:00:00Z"
}

Caching:
- Redis cached per category
```

#### 11. Upload Product Image
```http
POST /api/product/{productUuid}/images
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

Request Body (multipart):
- files: [image1.jpg, image2.jpg, ...]  // Max 5 images, 10MB each
- isPrimary: true  // Optional, for first/primary image

Response (201 Created):
{
  "uuid": "product-uuid-1",
  "images": [
    {
      "uuid": "image-uuid-1",
      "url": "https://cdn.example.com/products/product-1/img1.jpg",
      "isPrimary": true,
      "uploadedAt": "2026-02-15T12:10:00Z"
    },
    {
      "uuid": "image-uuid-2",
      "url": "https://cdn.example.com/products/product-1/img2.jpg",
      "isPrimary": false,
      "uploadedAt": "2026-02-15T12:10:05Z"
    }
  ]
}

Behavior:
- Images uploaded to CDN (AWS S3 / CloudFront)
- Auto-optimized (resizing, compression)
- Generates multiple resolutions (thumbnail, medium, large)
- Maximum 10 images per product

Authorization:
- SELLER role, own products only
- ADMIN can upload for any product

Errors:
- 400 Bad Request: Invalid file type (only JPEG, PNG)
- 400 Bad Request: File size > 10MB
- 400 Bad Request: Product already has 10 images
- 413 Payload Too Large: Request exceeds 50MB
```

#### 12. Delete Product Image
```http
DELETE /api/product/{productUuid}/images/{imageUuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Image deleted from CDN
- If was primary, next image becomes primary
- Cannot delete last remaining image (product must have at least 1)

Errors:
- 404 Not Found: Image/product not found
- 403 Forbidden: Not your product
- 400 Bad Request: Cannot delete last image
```

#### 13. List Product Images
```http
GET /api/product/{productUuid}/images
Authorization: (optional)

Response (200 OK):
{
  "productUuid": "product-uuid-1",
  "images": [
    {
      "uuid": "image-uuid-1",
      "url": "https://cdn.example.com/products/product-1/img1.jpg",
      "thumbnailUrl": "https://cdn.example.com/products/product-1/img1-thumb.jpg",
      "isPrimary": true,
      "uploadedAt": "2026-02-15T12:10:00Z"
    }
  ]
}

Features:
- Returns multiple image resolutions (thumbnail, medium, large)
- Primary image marked
```

#### 14. Create Product Variant
```http
POST /api/product/{productUuid}/variants
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "sku": "SAMSUNG-S24-256GB-BLACK",
  "name": "256GB Black",
  "attributes": {
    "storage": "256GB",
    "color": "Black"
  },
  "price": 79999.99,
  "stock": 50
}

Response (201 Created):
{
  "uuid": "variant-uuid-1",
  "productUuid": "product-uuid-1",
  "sku": "SAMSUNG-S24-256GB-BLACK",
  "name": "256GB Black",
  "attributes": {
    "storage": "256GB",
    "color": "Black"
  },
  "price": 79999.99,
  "stock": 50,
  "createdAt": "2026-02-15T12:00:00Z"
}

Behavior:
- Variants allow same product with different attributes
- Each variant has own SKU, price, stock
- Variants share reviews (aggregate at product level)

Authorization:
- SELLER role, own products only

Errors:
- 400 Bad Request: SKU already exists for this product
- 404 Not Found: Product not found
```

#### 15. Update Product Variant
```http
PUT /api/product/{productUuid}/variants/{variantUuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "256GB Black (Updated)",
  "price": 75999.99,
  "stock": 45
}

Response (200 OK):
{
  "uuid": "variant-uuid-1",
  "sku": "SAMSUNG-S24-256GB-BLACK",
  "name": "256GB Black (Updated)",
  "price": 75999.99,
  "stock": 45,
  "updatedAt": "2026-02-15T13:00:00Z"
}

Authorization:
- SELLER role, own products only
```

#### 16. Delete Product Variant
```http
DELETE /api/product/{productUuid}/variants/{variantUuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted=true)
- Cannot delete last variant (product must have at least 1)
- Existing orders with this variant unaffected

Errors:
- 400 Bad Request: Cannot delete last variant
- 404 Not Found: Variant not found
```

#### 17. List Product Variants
```http
GET /api/product/{productUuid}/variants
Authorization: (optional)

Response (200 OK):
{
  "productUuid": "product-uuid-1",
  "variants": [
    {
      "uuid": "variant-uuid-1",
      "sku": "SAMSUNG-S24-256GB-BLACK",
      "name": "256GB Black",
      "attributes": {
        "storage": "256GB",
        "color": "Black"
      },
      "price": 79999.99,
      "stock": 50,
      "createdAt": "2026-02-15T12:00:00Z"
    }
  ]
}
```

#### 18. Create Flash Deal (ADMIN only)
```http
POST /api/product/flash-deals
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "Weekend Flash Sale",
  "description": "50% off on selected electronics",
  "discountType": "PERCENTAGE",  // PERCENTAGE or FIXED
  "discountValue": 50.0,
  "products": [
    {
      "productUuid": "product-uuid-1",
      "stock": 20,
      "maxPerUser": 2
    }
  ],
  "startsAt": "2026-02-18T00:00:00Z",
  "endsAt": "2026-02-18T23:59:59Z"
}

Response (201 Created):
{
  "uuid": "flash-deal-uuid-1",
  "name": "Weekend Flash Sale",
  "description": "50% off on selected electronics",
  "discountType": "PERCENTAGE",
  "discountValue": 50.0,
  "status": "SCHEDULED",  // SCHEDULED, ACTIVE, COMPLETED
  "products": [
    {
      "productUuid": "product-uuid-1",
      "stockRemaining": 20,
      "maxPerUser": 2
    }
  ],
  "startsAt": "2026-02-18T00:00:00Z",
  "endsAt": "2026-02-18T23:59:59Z",
  "createdAt": "2026-02-15T12:00:00Z"
}

Behavior:
- Time-limited promotional deals
- Cron job auto-activates/deactivates based on time
- Stock limited per product
- Purchase limit per user enforced in order creation

Authorization:
- ADMIN role only
```

#### 19. Update Flash Deal (ADMIN only)
```http
PUT /api/product/flash-deals/{uuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "name": "Mega Flash Sale",
  "discountValue": 60.0,
  "endsAt": "2026-02-19T23:59:59Z"
}

Response (200 OK):
{
  "uuid": "flash-deal-uuid-1",
  "name": "Mega Flash Sale",
  "discountValue": 60.0,
  "updatedAt": "2026-02-15T13:00:00Z"
}

Constraints:
- Cannot update if ACTIVE (must wait for completion)
- Can extend end time before deal starts
```

#### 20. Delete Flash Deal (ADMIN only)
```http
DELETE /api/product/flash-deals/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted=true)
- Deal hidden from UI

Errors:
- 400 Bad Request: Cannot delete ACTIVE deal (would affect current orders)
```

#### 21. List Flash Deals
```http
GET /api/product/flash-deals?page=0&size=20&status=ACTIVE
Authorization: (optional)

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 20)
- status: SCHEDULED, ACTIVE, COMPLETED (optional)
- includeExpired: Show completed deals (default false)

Response (200 OK):
{
  "content": [
    {
      "uuid": "flash-deal-uuid-1",
      "name": "Weekend Flash Sale",
      "description": "50% off on selected electronics",
      "discountType": "PERCENTAGE",
      "discountValue": 50.0,
      "status": "ACTIVE",
      "productCount": 5,
      "startsAt": "2026-02-18T00:00:00Z",
      "endsAt": "2026-02-18T23:59:59Z",
      "timeRemaining": "4 hours 30 minutes"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "last": true
}

Caching:
- Redis cached (1-minute TTL for active deals)
```

#### 22. Get Active Flash Deals
```http
GET /api/product/flash-deals/active
Authorization: (optional)

Response (200 OK):
{
  "activeDeals": [
    {
      "uuid": "flash-deal-uuid-1",
      "name": "Weekend Flash Sale",
      "products": [
        {
          "uuid": "product-uuid-1",
          "name": "Samsung Galaxy S24",
          "originalPrice": 79999.99,
          "salePrice": 39999.99,
          "discount": "50%",
          "stockRemaining": 15,
          "timeRemaining": "2 hours 30 minutes"
        }
      ]
    }
  ]
}

Use Case:
- Homepage carousel showing active deals
- Real-time stock display
- Countdown timer info
```

#### 23. Get Inventory History
```http
GET /api/product/{productUuid}/inventory-history?page=0&size=20
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 20, max 100)
- action: CREATED, UPDATED, ORDERED, RETURNED, DAMAGED, RESTOCKED (optional)

Response (200 OK):
{
  "productUuid": "product-uuid-1",
  "productName": "Samsung Galaxy S24",
  "history": [
    {
      "uuid": "history-uuid-1",
      "action": "ORDERED",
      "previousStock": 50,
      "newStock": 48,
      "difference": 2,
      "reason": "Order #ORD-001 placed",
      "referenceUuid": "order-uuid-1",
      "performedBy": "system",
      "createdAt": "2026-02-15T14:00:00Z"
    },
    {
      "uuid": "history-uuid-2",
      "action": "UPDATED",
      "previousStock": 48,
      "newStock": 45,
      "reason": "Seller manual stock adjustment",
      "performedBy": "seller-uuid",
      "createdAt": "2026-02-15T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 127,
  "totalPages": 7,
  "last": false,
  "currentStock": 45
}

Authorization:
- SELLER: Own products only
- ADMIN: Any product

Use Case:
- Seller reconciliation of stock discrepancies
- Audit trail for inventory management
```

#### 24. Update Stock
```http
PUT /api/product/{productUuid}/stock
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "newStock": 100,
  "reason": "Supplier delivery received"
}

Response (200 OK):
{
  "uuid": "product-uuid-1",
  "previousStock": 45,
  "newStock": 100,
  "difference": 55,
  "historyEntryUuid": "history-uuid-3",
  "updatedAt": "2026-02-15T15:00:00Z"
}

Behavior:
- Manual stock update by seller
- Recorded in inventory history
- Atomic operation (prevents race conditions)

Authorization:
- SELLER: Own products only
- ADMIN: Any product

Errors:
- 400 Bad Request: New stock < 0
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

#### 6. Create Coupon (ADMIN only)
```http
POST /api/order/coupons
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "code": "WELCOME50",
  "description": "Welcome discount for new users",
  "discountType": "PERCENTAGE",  // PERCENTAGE or FIXED
  "discountValue": 50.0,
  "minimumAmount": 500.00,
  "maxUsagePerUser": 1,
  "maxTotalUsage": 1000,
  "validFrom": "2026-02-15T00:00:00Z",
  "validUntil": "2026-12-31T23:59:59Z",
  "applicableCategories": ["Electronics", "Fashion"],  // Optional
  "applicableToNewUsersOnly": true
}

Response (201 Created):
{
  "uuid": "coupon-uuid-1",
  "code": "WELCOME50",
  "description": "Welcome discount for new users",
  "discountType": "PERCENTAGE",
  "discountValue": 50.0,
  "minimumAmount": 500.00,
  "maxUsagePerUser": 1,
  "maxTotalUsage": 1000,
  "status": "ACTIVE",
  "validFrom": "2026-02-15T00:00:00Z",
  "validUntil": "2026-12-31T23:59:59Z",
  "usageCount": 0,
  "createdAt": "2026-02-15T12:00:00Z"
}

Behavior:
- Can be tiered (different discounts for different amounts)
- Auto-deactivated after expiry
- Limited usage tracking

Authorization:
- ADMIN role only
```

#### 7. Update Coupon (ADMIN only)
```http
PUT /api/order/coupons/{uuid}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "discountValue": 60.0,
  "maxTotalUsage": 500,
  "validUntil": "2026-06-30T23:59:59Z"
}

Response (200 OK):
{
  "uuid": "coupon-uuid-1",
  "code": "WELCOME50",
  "discountValue": 60.0,
  "maxTotalUsage": 500,
  "validUntil": "2026-06-30T23:59:59Z",
  "updatedAt": "2026-02-15T13:00:00Z"
}
```

#### 8. Delete Coupon (ADMIN only)
```http
DELETE /api/order/coupons/{uuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Soft delete (isDeleted=true)
- Existing orders with this coupon unaffected
- Cannot be used for new orders
```

#### 9. List Coupons (ADMIN)
```http
GET /api/order/coupons?page=0&size=20&status=ACTIVE
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 20)
- status: ACTIVE, INACTIVE, EXPIRED (optional)

Response (200 OK):
{
  "content": [
    {
      "uuid": "coupon-uuid-1",
      "code": "WELCOME50",
      "description": "Welcome discount for new users",
      "discountType": "PERCENTAGE",
      "discountValue": 50.0,
      "status": "ACTIVE",
      "usageCount": 245,
      "maxTotalUsage": 1000,
      "validUntil": "2026-12-31T23:59:59Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1,
  "last": true
}

Authorization:
- ADMIN role only
```

#### 10. Validate Coupon (BUYER)
```http
POST /api/order/coupons/validate
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "couponCode": "WELCOME50",
  "cartTotal": 5000.00
}

Response (200 OK):
{
  "couponUuid": "coupon-uuid-1",
  "code": "WELCOME50",
  "isValid": true,
  "discountValue": 2500.00,
  "finalAmount": 2500.00,
  "message": "Coupon applied successfully"
}

Validation Rules:
- Code exists and is ACTIVE
- Current date within validFrom and validUntil
- cartTotal >= minimumAmount
- User hasn't exceeded maxUsagePerUser
- Total usage < maxTotalUsage
- If new user only: User.createdAt recent (configurable, e.g., < 7 days)

Response (400 Bad Request - Invalid):
{
  "isValid": false,
  "message": "Coupon expired",
  "errorCode": "COUPON_EXPIRED"
}

Error Codes:
- COUPON_NOT_FOUND
- COUPON_EXPIRED
- MINIMUM_AMOUNT_NOT_MET
- USER_USAGE_LIMIT_EXCEEDED
- COUPON_USAGE_LIMIT_EXCEEDED
- NEW_USER_ONLY
```

#### 11. Deactivate Coupon (ADMIN only)
```http
PUT /api/order/coupons/{uuid}/deactivate
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "uuid": "coupon-uuid-1",
  "code": "WELCOME50",
  "status": "INACTIVE",
  "deactivatedAt": "2026-02-15T14:00:00Z"
}

Behavior:
- Quick way to disable a coupon
- Different from delete (audit trail preserved)
```

#### 12. Request Return (BUYER)
```http
POST /api/order/{orderUuid}/returns
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "items": [
    {
      "productUuid": "product-uuid-1",
      "quantity": 1,
      "reason": "DEFECTIVE"  // DEFECTIVE, WRONG_ITEM, NOT_AS_DESCRIBED, CHANGED_MIND, OTHER
    }
  ],
  "additionalComments": "Product screen is cracked"
}

Response (201 Created):
{
  "uuid": "return-uuid-1",
  "orderUuid": "order-uuid-1",
  "status": "REQUESTED",
  "items": [
    {
      "productUuid": "product-uuid-1",
      "quantity": 1,
      "reason": "DEFECTIVE"
    }
  ],
  "requestedAt": "2026-02-15T15:00:00Z"
}

Constraints:
- Order must be DELIVERED
- Return window: 7-30 days after delivery (configurable)
- Cannot return already returned items

Errors:
- 400 Bad Request: Return window expired
- 400 Bad Request: Order not DELIVERED
- 409 Conflict: Item already returned
```

#### 13. Approve Return (SELLER/ADMIN)
```http
PUT /api/order/returns/{uuid}/approve
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "rmaNumber": "RMA-2026-0001",
  "instructions": "Please ship to warehouse address: 123 Return St",
  "refundAmount": 79999.99
}

Response (200 OK):
{
  "uuid": "return-uuid-1",
  "status": "APPROVED",
  "rmaNumber": "RMA-2026-0001",
  "instructions": "Please ship to warehouse address: 123 Return St",
  "approvedAt": "2026-02-15T15:30:00Z"
}

Behavior:
- Email sent to buyer with RMA number and shipping instructions
- Return window opens (buyer has 14 days to ship)
- Refund amount confirmed

Authorization:
- SELLER: Can approve returns for own sold items
- ADMIN: Can approve any return
```

#### 14. Reject Return (SELLER/ADMIN)
```http
PUT /api/order/returns/{uuid}/reject
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "rejectionReason": "Item outside return window"
}

Response (200 OK):
{
  "uuid": "return-uuid-1",
  "status": "REJECTED",
  "rejectionReason": "Item outside return window",
  "rejectedAt": "2026-02-15T15:30:00Z"
}

Behavior:
- Email sent to buyer explaining rejection
- No refund processed
```

#### 15. Update Return Status
```http
PUT /api/order/returns/{uuid}/status?status=ITEM_RECEIVED
Authorization: Bearer {accessToken}

Query Parameter:
- status: REQUESTED, APPROVED, ITEM_IN_TRANSIT, ITEM_RECEIVED, REFUNDED, REJECTED, CANCELLED

Response (200 OK):
{
  "uuid": "return-uuid-1",
  "status": "ITEM_RECEIVED",
  "updatedAt": "2026-02-15T16:00:00Z"
}

State Machine:
REQUESTED → APPROVED → ITEM_IN_TRANSIT → ITEM_RECEIVED → REFUNDED
        ↓
     REJECTED

Authorization:
- SELLER/ADMIN can transition forward
- BUYER can cancel (only if REQUESTED or APPROVED)
```

#### 16. List Returns
```http
GET /api/order/returns?page=0&size=10&status=APPROVED
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 10, max 50)
- status: REQUESTED, APPROVED, ITEM_IN_TRANSIT, ITEM_RECEIVED, REFUNDED, REJECTED (optional)
- orderUuid: Filter by order (optional)

Response (200 OK):
{
  "content": [
    {
      "uuid": "return-uuid-1",
      "orderUuid": "order-uuid-1",
      "buyerName": "John Doe",
      "itemCount": 1,
      "refundAmount": 79999.99,
      "status": "APPROVED",
      "rmaNumber": "RMA-2026-0001",
      "requestedAt": "2026-02-15T15:00:00Z",
      "approvedAt": "2026-02-15T15:30:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}

Authorization:
- BUYER: Own returns only
- SELLER: Returns for own sold items
- ADMIN: All returns
```

#### 17. Create Shipment Tracking Event
```http
POST /api/order/{orderUuid}/shipment-events
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "status": "SHIPPED",           // SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
  "location": "Mumbai Hub",
  "timestamp": "2026-02-15T16:00:00Z",
  "description": "Order picked up from warehouse"
}

Response (201 Created):
{
  "uuid": "tracking-event-uuid-1",
  "orderUuid": "order-uuid-1",
  "status": "SHIPPED",
  "location": "Mumbai Hub",
  "timestamp": "2026-02-15T16:00:00Z",
  "description": "Order picked up from warehouse",
  "createdAt": "2026-02-15T16:00:00Z"
}

Behavior:
- Created by logistics partner
- Auto-syncs with shipping provider (third-party API)
- Updates order status if status reaches DELIVERED

Authorization:
- Logistics service (internal)
- ADMIN can create manually
```

#### 18. List Shipment Events
```http
GET /api/order/{orderUuid}/shipment-events
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "orderUuid": "order-uuid-1",
  "events": [
    {
      "uuid": "tracking-event-uuid-1",
      "status": "DELIVERED",
      "location": "New York Delivery Hub",
      "timestamp": "2026-02-16T14:00:00Z",
      "description": "Delivered to customer"
    },
    {
      "uuid": "tracking-event-uuid-0",
      "status": "OUT_FOR_DELIVERY",
      "location": "New York",
      "timestamp": "2026-02-16T09:00:00Z",
      "description": "Out for delivery"
    }
  ]
}

Authorization:
- BUYER: Own orders only
- SELLER: Orders with their items
- ADMIN: Any order
```

#### 19. Get Latest Shipment Location
```http
GET /api/order/{orderUuid}/shipment-location
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "orderUuid": "order-uuid-1",
  "latestStatus": "OUT_FOR_DELIVERY",
  "location": "New York",
  "timestamp": "2026-02-16T09:00:00Z",
  "estimatedDelivery": "2026-02-16T18:00:00Z",
  "trackingUrl": "https://carrier.com/track/12345"
}

Use Case:
- Order tracking page
- Show customer current status
- Estimated delivery time
```

#### 20. Download Invoice (PDF)
```http
GET /api/order/{orderUuid}/invoice
Authorization: Bearer {accessToken}

Response (200 OK - PDF):
Content-Type: application/pdf
Content-Disposition: attachment; filename="invoice-ORD-001.pdf"

[PDF Binary Data]
```

Invoice Contents:
- Order number, date, delivery date
- Buyer details (name, address)
- Seller details (name, GST number)
- Item list (product name, quantity, price, tax)
- Subtotal, taxes, shipping, discounts, total
- Payment method  
- QR code linking to order

Use Case:
- Buyer downloads for records/warranty
- Seller uses for accounting
- GST compliance

Authorization:
- BUYER: Own orders only
- SELLER: Orders with their items
- ADMIN: Any order

Errors:
- 404 Not Found: Order not found
- 400 Bad Request: Order not yet delivered (some systems don't generate until delivered)
```

#### 21. Admin Dashboard
```http
GET /api/order/admin/dashboard
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "summary": {
    "totalOrders": 5432,
    "totalRevenue": 54320000.00,
    "averageOrderValue": 10000.00,
    "todayOrders": 47,
    "todayRevenue": 470000.00
  },
  "orderStatusBreakdown": {
    "CREATED": 123,
    "CONFIRMED": 256,
    "SHIPPED": 342,
    "DELIVERED": 4711
  },
  "paymentStatusBreakdown": {
    "SUCCESS": 5200,
    "PENDING": 200,
    "FAILED": 32
  },
  "topProducts": [
    {
      "productUuid": "product-1",
      "name": "Samsung Galaxy S24",
      "quantity": 456,
      "revenue": 36000000.00
    }
  ],
  "topSellers": [
    {
      "sellerUuid": "seller-1",
      "businessName": "Tech Store",
      "orders": 234,
      "revenue": 5670000.00
    }
  ],
  "revenueByDay": [
    {
      "date": "2026-02-15",
      "revenue": 1000000.00,
      "orders": 100
    }
  ],
  "returnRate": "2.3%",
  "cancellationRate": "0.8%"
}

Authorization:
- ADMIN role only

Use Case:
- Finance/Operations dashboard
- Real-time metrics
- Trend analysis
```

#### 22. Seller Dashboard
```http
GET /api/order/seller/dashboard
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "summary": {
    "totalOrders": 234,
    "totalRevenue": 5670000.00,
    "averageOrderValue": 24232.07,
    "thisWeekOrders": 45,
    "thisWeekRevenue": 1110000.00
  },
  "orderStatusBreakdown": {
    "CREATED": 10,
    "CONFIRMED": 23,
    "SHIPPED": 45,
    "DELIVERED": 156
  },
  "topProducts": [
    {
      "productUuid": "product-1",
      "name": "Samsung Galaxy S24",
      "quantity": 120,
      "revenue": 2400000.00
    }
  ],
  "pendingOrders": [
    {
      "orderUuid": "order-1",
      "buyerName": "John Doe",
      "itemCount": 2,
      "amount": 50000.00,
      "status": "CONFIRMED",
      "createdAt": "2026-02-15T14:00:00Z"
    }
  ],
  "recentReviews": [
    {
      "productUuid": "product-1",
      "buyerName": "Jane Smith",
      "rating": 5,
      "comment": "Great product!"
    }
  ],
  "rating": 4.7,
  "reviewCount": 564
}

Authorization:
- SELLER role, own data only

Use Case:
- Seller performance tracking
- Fulfillment queue
- Customer feedback
```

#### 23. List Audit Logs
```http
GET /api/order/audit-logs?page=0&size=20&entityType=ORDER
Authorization: Bearer {accessToken}

Query Parameters:
- page: Zero-indexed page
- size: Per page (default 20, max 100)
- entityType: ORDER, RETURN, COUPON (optional)
- action: CREATE, UPDATE, DELETE (optional)
- startDate: ISO 8601 datetime (optional)
- endDate: ISO 8601 datetime (optional)

Response (200 OK):
{
  "content": [
    {
      "uuid": "audit-log-uuid-1",
      "entityType": "ORDER",
      "entityUuid": "order-uuid-1",
      "action": "UPDATE",
      "previousState": {
        "status": "CREATED",
        "paymentStatus": "PENDING"
      },
      "newState": {
        "status": "CONFIRMED",
        "paymentStatus": "SUCCESS"
      },
      "changedBy": "payment-service",  // System identifier or user UUID
      "timestamp": "2026-02-15T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45678,
  "totalPages": 2284,
  "last": false
}

Authorization:
- ADMIN role only

Use Case:
- Compliance/legal requirements
- Debugging order state issues
- Fraud investigation
```

#### 24. Search Audit Logs by Entity
```http
GET /api/order/audit-logs/search?entityUuid={entityUuid}
Authorization: Bearer {accessToken}

Query Parameter:
- entityUuid: Order UUID, return UUID, or coupon UUID

Response (200 OK):
{
  "entityUuid": "order-uuid-1",
  "entityType": "ORDER",
  "logs": [
    {
      "uuid": "audit-log-uuid-1",
      "action": "CREATE",
      "previousState": null,
      "newState": {
        "status": "CREATED",
        "paymentStatus": "PENDING"
      },
      "changedBy": "buyer-uuid-1",
      "timestamp": "2026-02-15T14:00:00Z"
    },
    {
      "uuid": "audit-log-uuid-2",
      "action": "UPDATE",
      "previousState": {
        "status": "CREATED",
        "paymentStatus": "PENDING"
      },
      "newState": {
        "status": "CONFIRMED",
        "paymentStatus": "SUCCESS"
      },
      "changedBy": "payment-service",
      "timestamp": "2026-02-15T15:00:00Z"
    }
  ]
}

Use Case:
- Complete timeline of entity changes
- State transition audit for disputes
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

#### 6. Upload Review Images
```http
POST /api/review/{reviewUuid}/images
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

Request Body (multipart):
- files: [image1.jpg, image2.jpg, ...]  // Max 5 images per review, 5MB each
- imageIndex: [0, 1, ...]  // Optional position index (default appends)

Response (201 Created):
{
  "uuid": "review-uuid-1",
  "images": [
    {
      "uuid": "review-image-uuid-1",
      "url": "https://cdn.example.com/reviews/review-1/img1.jpg",
      "thumbnailUrl": "https://cdn.example.com/reviews/review-1/img1-thumb.jpg",
      "uploadedAt": "2026-02-15T16:10:00Z"
    },
    {
      "uuid": "review-image-uuid-2",
      "url": "https://cdn.example.com/reviews/review-1/img2.jpg",
      "thumbnailUrl": "https://cdn.example.com/reviews/review-1/img2-thumb.jpg",
      "uploadedAt": "2026-02-15T16:10:05Z"
    }
  ]
}

Behavior:
- Images uploaded to CDN
- Auto-optimized (compression, resizing)
- Generates thumbnail + full resolution
- Validated against review ownership

Authorization:
- BUYER role required
- Own reviews only

Constraints:
- Max 5 images per review
- File size: 5MB each
- Format: JPEG, PNG, WebP only

Errors:
- 404 Not Found: Review not found
- 403 Forbidden: Not your review
- 400 Bad Request: File type not supported
- 413 Payload Too Large: File exceeds 5MB
```

#### 7. Delete Review Image
```http
DELETE /api/review/{reviewUuid}/images/{imageUuid}
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Image deleted from CDN and database
- Cannot delete last image if review is published

Authorization:
- BUYER role required
- Own reviews only

Errors:
- 404 Not Found: Image/review not found
- 403 Forbidden: Not your review
```

#### 8. Vote Helpful (Helpful Vote)
```http
POST /api/review/{reviewUuid}/helpful
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "isHelpful": true  // true for helpful, false for not helpful
}

Response (201 Created):
{
  "reviewUuid": "review-uuid-1",
  "totalHelpfulVotes": 15,
  "totalNotHelpfulVotes": 2,
  "userVote": "HELPFUL",  // HELPFUL, NOT_HELPFUL, NONE
  "votedAt": "2026-02-15T17:00:00Z"
}

Behavior:
- Buyer votes if review was helpful
- Can change vote (HELPFUL ↔ NOT_HELPFUL)
- Cannot vote on own reviews
- One vote per buyer per review

Side Effects:
- Updates review helpfulness score
- Affects review ranking in product page
- Most helpful reviews shown first

Errors:
- 403 Forbidden: Cannot vote on own review
- 404 Not Found: Review not found
- 400 Bad Request: Already voted (use PUT instead)
```

#### 9. Remove Helpful Vote
```http
DELETE /api/review/{reviewUuid}/helpful
Authorization: Bearer {accessToken}

Response (204 No Content):
(empty body)

Behavior:
- Removes the vote (doesn't matter if HELPFUL or NOT_HELPFUL)
- Vote count decrements

Errors:
- 404 Not Found: Review not found
- 400 Bad Request: User has not voted on this review
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
