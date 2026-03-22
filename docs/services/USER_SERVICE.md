# User Service — API Documentation

**Base URL:** `http://localhost:8080/api/user`  
**Internal Port:** 8080  
**Database:** user_db (PostgreSQL)  
**Cache:** Redis (15-min TTL)

---

## Table of Contents

- [Overview](#overview)
- [User Management](#user-management)
  - [POST /register](#post-register)
  - [POST /verify-otp](#post-verify-otp)
  - [POST /resend-otp](#post-resend-otp)
  - [GET /me](#get-me)
  - [PUT /me](#put-me)
  - [PUT /me/change-password](#put-mechange-password)
  - [POST /me/seller-details](#post-meseller-details)
- [Addresses](#addresses)
  - [GET /me/addresses](#get-meaddresses)
  - [POST /me/addresses](#post-meaddresses)
  - [PUT /me/addresses/{uuid}](#put-meaddressesuuid)
  - [DELETE /me/addresses/{uuid}](#delete-meaddressesuuid)
  - [PUT /me/addresses/{uuid}/default](#put-meaddressesuuiddefault)
- [Cart](#cart)
  - [GET /cart](#get-cart)
  - [POST /cart](#post-cart)
  - [PUT /cart/{itemId}](#put-cartitemid)
  - [DELETE /cart/{itemId}](#delete-cartitemid)
  - [DELETE /cart](#delete-cart)
- [Wishlist](#wishlist)
  - [GET /wishlist](#get-wishlist)
  - [POST /wishlist](#post-wishlist)
  - [DELETE /wishlist/{productUuid}](#delete-wishlistproductuuid)
  - [GET /wishlist/check/{productUuid}](#get-wishlistcheckproductuuid)
- [Notifications](#notifications)
  - [GET /notifications](#get-notifications)
  - [GET /notifications/unread](#get-notificationsunread)
  - [GET /notifications/unread-count](#get-notificationsunread-count)
  - [PUT /notifications/{uuid}/read](#put-notificationsuuidread)
  - [PUT /notifications/read-all](#put-notificationsread-all)
- [Loyalty Program](#loyalty-program)
  - [GET /loyalty/balance](#get-loyaltybalance)
  - [GET /loyalty/history](#get-loyaltyhistory)
  - [POST /loyalty/redeem](#post-loyaltyredeem)
  - [POST /loyalty/award](#post-loyaltyaward)
- [Support Tickets](#support-tickets)
  - [POST /support](#post-support)
  - [GET /support/{uuid}](#get-supportuuid)
  - [GET /support/me](#get-supportme)
  - [GET /support (admin)](#get-support-admin)
  - [GET /support/status/{status}](#get-supportstatusstatus)
  - [POST /support/{uuid}/message](#post-supportuuidmessage)
  - [PUT /support/{uuid}/status](#put-supportuuidstatus)
  - [PUT /support/{uuid}/assign](#put-supportuuidassign)
  - [POST /support/{uuid}/close](#post-supportuuidclose)
- [Admin Endpoints](#admin-endpoints)
- [Error Responses](#error-responses)
- [Database Schema](#database-schema)

---

## Overview

The User Service is the largest service in the platform. It manages user accounts, addresses, shopping cart, wishlist, notifications, loyalty points, and customer support tickets. It also handles seller onboarding and admin user management.

**User Status Lifecycle:**
```
PENDING_VERIFICATION → (verify OTP) → ACTIVE (buyer)
PENDING_VERIFICATION → (verify OTP) → PENDING_DETAILS (seller)
PENDING_DETAILS → (submit seller details) → PENDING_APPROVAL
PENDING_APPROVAL → (admin approves) → ACTIVE
ACTIVE → (admin blocks) → BLOCKED
BLOCKED → (admin unblocks) → ACTIVE
```

---

## User Management

---

### POST /register

Create a new user account. Sends a 6-digit OTP to the email for verification.

**Auth Required:** No (Public)

```http
POST /api/user/register
Content-Type: application/json
```

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "securePass123",
  "phoneNumber": "9876543210",
  "role": "BUYER"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| firstName | string | Yes | 1–50 chars |
| lastName | string | Yes | 1–50 chars |
| email | string | Yes | Valid email, unique |
| password | string | Yes | 8–100 chars |
| phoneNumber | string | Yes | 10 digits (`^[0-9]{10}$`) |
| role | string | Yes | `BUYER`, `SELLER`, or `ADMIN` |

**Response (201 Created):**

```json
{
  "success": true,
  "message": "User registered successfully. Please verify your email.",
  "data": {
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "role": "BUYER",
    "status": "PENDING_VERIFICATION",
    "emailVerified": false,
    "approved": false
  }
}
```

| Status | Condition |
|:------:|-----------|
| 201 | User created, OTP sent |
| 409 | Email already registered |
| 400 | Validation errors |


---

### POST /verify-otp

Verify email using the OTP sent during registration.

**Auth Required:** No (Public)

```http
POST /api/user/verify-otp
Content-Type: application/json
```

```json
{
  "email": "john@example.com",
  "otpCode": "482910"
}
```

| Field | Type | Required |
|-------|------|:--------:|
| email | string | Yes |
| otpCode | string | Yes |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": null
}
```

| Status | Condition |
|:------:|-----------|
| 200 | Email verified |
| 400 | Invalid or expired OTP |


---

### POST /resend-otp

Resend the verification OTP to the user's email.

**Auth Required:** No (Public)

```http
POST /api/user/resend-otp?email=john@example.com
```

| Parameter | Location | Type | Required |
|-----------|----------|------|:--------:|
| email | Query | string | Yes |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "OTP resent successfully",
  "data": null
}
```


---

### GET /me

Get the authenticated user's profile.

**Auth Required:** Yes (Any authenticated user)  
**Header:** `Authorization: Bearer <accessToken>`

```http
GET /api/user/me
Authorization: Bearer eyJhbGci...
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Profile retrieved",
  "data": {
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "role": "BUYER",
    "status": "ACTIVE",
    "emailVerified": true,
    "approved": false
  }
}
```


---

### PUT /me

Update the authenticated user's profile.

**Auth Required:** Yes (Any authenticated user)

```http
PUT /api/user/me
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "9876543211"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| firstName | string | No | 1–50 chars |
| lastName | string | No | 1–50 chars |
| phoneNumber | string | No | Max 15 chars |

**Response (200 OK):** Updated `UserResponse` (same structure as GET /me)


---

### PUT /me/change-password

Change the authenticated user's password.

**Auth Required:** Yes (Any authenticated user)

```http
PUT /api/user/me/change-password
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePass456",
  "confirmNewPassword": "newSecurePass456"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| currentPassword | string | Yes | Not blank |
| newPassword | string | Yes | Min 8 chars |
| confirmNewPassword | string | Yes | Must match newPassword |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null
}
```

| Status | Condition |
|:------:|-----------|
| 200 | Password changed |
| 400 | Current password wrong, or passwords don't match |


---

### POST /me/seller-details

Submit seller verification documents (for users registered with role=SELLER).

**Auth Required:** Yes (SELLER only)

```http
POST /api/user/me/seller-details
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "businessName": "Johns Electronics",
  "businessType": "INDIVIDUAL",
  "gstNumber": "22AAAAA0000A1Z5",
  "panNumber": "AAAPZ1234C",
  "addressLine1": "123 MG Road",
  "addressLine2": "Suite 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "bankAccountName": "John Doe",
  "bankAccountNumber": "1234567890123456",
  "bankIfsc": "SBIN0001234"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| businessName | string | Yes | 2–200 chars |
| businessType | string | Yes | INDIVIDUAL, COMPANY, or PARTNERSHIP |
| gstNumber | string | No | Max 20 chars |
| panNumber | string | No | Max 15 chars |
| addressLine1 | string | Yes | 5–255 chars |
| addressLine2 | string | No | Max 255 chars |
| city | string | Yes | Letters only, max 100 chars |
| state | string | Yes | Max 100 chars |
| pincode | string | Yes | 5–10 chars |
| bankAccountName | string | Yes | 5–100 chars |
| bankAccountNumber | string | Yes | Not blank |
| bankIfsc | string | Yes | IFSC format |

**Response (201 Created):**

```json
{
  "success": true,
  "message": "Seller details submitted for approval",
  "data": {
    "uuid": "660f9500-f30c-52e5-b827-557766550000",
    "businessName": "Johns Electronics",
    "businessType": "INDIVIDUAL",
    "submittedAt": "2026-03-06T12:00:00"
  }
}
```

User status changes: `PENDING_DETAILS → PENDING_APPROVAL`


---

## Addresses

---

### GET /me/addresses

List all addresses for the authenticated user.

**Auth Required:** Yes (Any authenticated user)

```http
GET /api/user/me/addresses
Authorization: Bearer eyJhbGci...
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": [
    {
      "uuid": "addr-uuid-1",
      "label": "Home",
      "fullName": "John Doe",
      "phone": "9876543210",
      "addressLine1": "123 MG Road",
      "addressLine2": "Apt 4B",
      "city": "Mumbai",
      "state": "Maharashtra",
      "pincode": "400001",
      "isDefault": true,
      "createdAt": "2026-03-06T12:00:00"
    }
  ]
}
```


---

### POST /me/addresses

Add a new address.

**Auth Required:** Yes (Any authenticated user)

```json
{
  "label": "Office",
  "fullName": "John Doe",
  "phone": "9876543210",
  "addressLine1": "456 Business Park",
  "addressLine2": "Floor 3",
  "city": "Pune",
  "state": "Maharashtra",
  "pincode": "411001",
  "isDefault": false
}
```

| Field | Type | Required |
|-------|------|:--------:|
| label | string | No |
| fullName | string | Yes |
| phone | string | Yes |
| addressLine1 | string | Yes |
| addressLine2 | string | No |
| city | string | Yes |
| state | string | Yes |
| pincode | string | Yes |
| isDefault | boolean | No |

**Response (201 Created):** `AddressResponse`


---

### PUT /me/addresses/{uuid}

Update an existing address.

```http
PUT /api/user/me/addresses/addr-uuid-1
```

Request body: Same as POST. Response: Updated `AddressResponse`.


---

### DELETE /me/addresses/{uuid}

Delete an address.

```http
DELETE /api/user/me/addresses/addr-uuid-1
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Address deleted"
}
```


---

### PUT /me/addresses/{uuid}/default

Set an address as the default.

```http
PUT /api/user/me/addresses/addr-uuid-1/default
```

**Response (200 OK):** Updated `AddressResponse` with `isDefault: true`. All other addresses are set to `isDefault: false`.


---

## Cart

---

### GET /cart

Get the authenticated buyer's cart contents.

**Auth Required:** Yes (BUYER only)

```http
GET /api/user/cart
Authorization: Bearer eyJhbGci...
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "itemId": 1,
        "productUuid": "prod-uuid-1",
        "productName": "Laptop Pro X",
        "productImage": "https://example.com/laptop.jpg",
        "price": 79999.00,
        "quantity": 1,
        "subtotal": 79999.00
      }
    ],
    "totalItems": 1,
    "totalAmount": 79999.00
  }
}
```


---

### POST /cart

Add an item to the cart. If the product already exists in the cart, the quantity is incremented.

**Auth Required:** Yes (BUYER only)

```json
{
  "productUuid": "prod-uuid-1",
  "productName": "Laptop Pro X",
  "productImage": "https://example.com/laptop.jpg",
  "price": 79999.00,
  "quantity": 1
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| productUuid | string | Yes | Not blank |
| productName | string | Yes | Denormalized |
| productImage | string | Yes | Denormalized |
| price | double | Yes | Positive |
| quantity | int | No | Min 1 (default: 1) |

**Response (200 OK):** Updated `CartResponse`


---

### PUT /cart/{itemId}

Update the quantity of a cart item.

```http
PUT /api/user/cart/1?quantity=3
```

| Parameter | Location | Type | Required |
|-----------|----------|------|:--------:|
| quantity | Query | int | Yes |

**Response (200 OK):** Updated `CartResponse`


---

### DELETE /cart/{itemId}

Remove a specific item from the cart.

```http
DELETE /api/user/cart/1
```

**Response (200 OK):** Updated `CartResponse`


---

### DELETE /cart

Clear the entire cart.

```http
DELETE /api/user/cart
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Cart cleared"
}
```


---

## Wishlist

---

### GET /wishlist

List all wishlist items.

**Auth Required:** Yes (BUYER only)

```http
GET /api/user/wishlist
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "productUuid": "prod-uuid-1",
      "productName": "Laptop Pro X",
      "productImage": "https://example.com/laptop.jpg",
      "price": 79999.00,
      "createdAt": "2026-03-06T12:00:00"
    }
  ]
}
```


---

### POST /wishlist

Add a product to the wishlist.

**Auth Required:** Yes (BUYER only)

```json
{
  "productUuid": "prod-uuid-1",
  "productName": "Laptop Pro X",
  "productImage": "https://example.com/laptop.jpg",
  "price": 79999.00
}
```

**Response (200 OK):** Updated wishlist list


---

### DELETE /wishlist/{productUuid}

Remove a product from the wishlist.

```http
DELETE /api/user/wishlist/prod-uuid-1
```

**Response (200 OK):** Updated wishlist list


---

### GET /wishlist/check/{productUuid}

Check if a product is in the user's wishlist.

```http
GET /api/user/wishlist/check/prod-uuid-1
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": true
}
```


---

## Notifications

---

### GET /notifications

List all notifications (paginated).

**Auth Required:** Yes (Any role)

```http
GET /api/user/notifications?page=0&size=20
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "uuid": "notif-uuid-1",
      "userUuid": "user-uuid",
      "title": "Order Confirmed",
      "message": "Your order #ABC has been confirmed",
      "type": "ORDER",
      "isRead": false,
      "createdAt": "2026-03-06T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```


---

### GET /notifications/unread

List only unread notifications (paginated).

```http
GET /api/user/notifications/unread?page=0&size=20
```


---

### GET /notifications/unread-count

Get the count of unread notifications.

```http
GET /api/user/notifications/unread-count
```

**Response (200 OK):**

```json
{
  "unreadCount": 5
}
```


---

### PUT /notifications/{uuid}/read

Mark a single notification as read.

```http
PUT /api/user/notifications/notif-uuid-1/read
```


---

### PUT /notifications/read-all

Mark all notifications as read.

```http
PUT /api/user/notifications/read-all
```


---

## Loyalty Program

---

### GET /loyalty/balance

Get the current loyalty points balance and tier.

**Auth Required:** Yes (BUYER only)

```http
GET /api/user/loyalty/balance
```

**Response (200 OK):**

```json
{
  "userUuid": "user-uuid",
  "balance": 2500,
  "expiringPoints": 200,
  "expiryDate": "2026-06-01T00:00:00"
}
```

**Tier levels:** BRONZE (0–999), SILVER (1000–4999), GOLD (5000–9999), PLATINUM (10000+)


---

### GET /loyalty/history

Get loyalty points transaction history (paginated).

```http
GET /api/user/loyalty/history?page=0&size=20
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "uuid": "txn-uuid-1",
      "points": 100,
      "transactionType": "PURCHASE",
      "description": "Points earned for order ORD-123",
      "reference": "order-uuid-1",
      "createdAt": "2026-03-06T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```


---

### POST /loyalty/redeem

Redeem loyalty points for a discount on an order.

**Auth Required:** Yes (BUYER only)

```json
{
  "points": 500,
  "orderUuid": "order-uuid-1"
}
```

**Response (200 OK):**

```json
{
  "pointsRedeemed": 500,
  "discountAmount": 500.00
}
```

1 point = ₹1 discount.


---

### POST /loyalty/award

Award loyalty points to a user (admin only).

**Auth Required:** Yes (ADMIN only)

```json
{
  "userUuid": "user-uuid",
  "points": 1000,
  "description": "Bonus points for being awesome"
}
```

**Response (200 OK):** Confirmation string


---

## Support Tickets

---

### POST /support

Create a new support ticket.

**Auth Required:** Yes (BUYER or SELLER)

```json
{
  "subject": "Order not received",
  "description": "I placed order ORD-123 a week ago but haven't received it yet.",
  "category": "ORDER",
  "orderUuid": "order-uuid-1"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| subject | string | Yes | Max 200 chars |
| description | string | Yes | Max 2000 chars |
| category | string | No | ORDER, PAYMENT, ACCOUNT, PRODUCT, SHIPPING, OTHER |
| orderUuid | string | No | Related order UUID |

**Response (201 Created):**

```json
{
  "uuid": "ticket-uuid-1",
  "userUuid": "user-uuid",
  "subject": "Order not received",
  "description": "I placed order ORD-123...",
  "category": "ORDER",
  "status": "OPEN",
  "orderUuid": "order-uuid-1",
  "assignedAdminUuid": null,
  "messages": [],
  "createdAt": "2026-03-06T12:00:00",
  "resolvedAt": null
}
```


---

### GET /support/{uuid}

Get support ticket details with all messages.

```http
GET /api/user/support/ticket-uuid-1
```

**Response (200 OK):** `TicketResponse` with messages array


---

### GET /support/me

List the authenticated user's tickets (paginated).

```http
GET /api/user/support/me?page=0&size=10
```


---

### GET /support (admin)

List all support tickets (admin only, paginated).

**Auth Required:** Yes (ADMIN only)

```http
GET /api/user/support?page=0&size=10
```


---

### GET /support/status/{status}

Filter tickets by status (admin only).

```http
GET /api/user/support/status/OPEN?page=0&size=10
```

Status values: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`


---

### POST /support/{uuid}/message

Post a message on a ticket.

**Auth Required:** Yes (BUYER, SELLER, or ADMIN)

```json
{
  "content": "I've checked and can see the shipment is delayed. Let me escalate this."
}
```

**Response (200 OK):** Updated `TicketResponse` with new message


---

### PUT /support/{uuid}/status

Update ticket status (admin only).

```http
PUT /api/user/support/ticket-uuid-1/status?status=IN_PROGRESS
```


---

### PUT /support/{uuid}/assign

Assign ticket to the current admin.

```http
PUT /api/user/support/ticket-uuid-1/assign
```


---

### POST /support/{uuid}/close

Close a ticket (admin only).

```http
POST /api/user/support/ticket-uuid-1/close
```

**Response (200 OK):** `TicketResponse` with status `CLOSED`


---

## Admin Endpoints

**Auth Required:** Yes (ADMIN only)  
**Base Path:** `/api/user/admin`

| Method | Endpoint | Description |
|:------:|----------|-------------|
| GET | `/users` | List all users (paginated) |
| GET | `/users/search?q=john` | Search users by name/email |
| PUT | `/block/{uuid}` | Block user (ACTIVE → BLOCKED) |
| PUT | `/unblock/{uuid}` | Unblock user (BLOCKED → ACTIVE) |
| PUT | `/approve/{uuid}` | Approve seller (PENDING_APPROVAL → ACTIVE) |
| GET | `/pending-sellers` | List sellers awaiting approval |


---

## Error Responses

All error responses follow the `ApiResponse` format:

```json
{
  "success": false,
  "message": "Email already registered",
  "data": null
}
```

| Status | Common Causes |
|:------:|---------------|
| 400 | Validation failure, bad OTP, password mismatch |
| 401 | Missing/invalid JWT |
| 403 | Insufficient role |
| 404 | User/address/ticket not found |
| 409 | Email already exists |

---

## Database Schema

### users

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| first_name | VARCHAR(50) | NOT NULL |
| last_name | VARCHAR(50) | NOT NULL |
| email | VARCHAR(100) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt) |
| phone_number | VARCHAR(15) | |
| role | VARCHAR | BUYER, SELLER, ADMIN |
| status | VARCHAR | User status enum |
| email_verified | BOOLEAN | |
| is_approved | BOOLEAN | |
| is_deleted | BOOLEAN | |
| last_login_at | TIMESTAMP | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### addresses

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| user_id | BIGINT | FK → users |
| label | VARCHAR | |
| full_name | VARCHAR | NOT NULL |
| phone | VARCHAR(15) | NOT NULL |
| address_line1 | VARCHAR(255) | NOT NULL |
| address_line2 | VARCHAR(255) | |
| city | VARCHAR(100) | NOT NULL |
| state | VARCHAR(100) | NOT NULL |
| pincode | VARCHAR(10) | NOT NULL |
| is_default | BOOLEAN | |

### cart_items

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| user_id | BIGINT (FK → users) |
| product_uuid | VARCHAR |
| product_name | VARCHAR |
| product_image | VARCHAR |
| price | DOUBLE |
| quantity | INT |
| added_at | TIMESTAMP |

### wishlist_items

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| user_id | BIGINT (FK → users) |
| product_uuid | VARCHAR |
| product_name | VARCHAR |
| product_image | VARCHAR |
| price | DOUBLE |
| created_at | TIMESTAMP |

### otp_verifications

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| email | VARCHAR |
| otp_code | VARCHAR |
| type | VARCHAR (EMAIL_VERIFICATION / PASSWORD_RESET) |
| expiry_time | TIMESTAMP (10 min from creation) |
| is_used | BOOLEAN |

### notifications

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| user_id | BIGINT (FK → users) |
| title | VARCHAR |
| message | VARCHAR |
| type | VARCHAR (ORDER, REVIEW, LOYALTY, SUPPORT) |
| is_read | BOOLEAN |
| created_at | TIMESTAMP |

### loyalty_points

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| user_id | BIGINT (FK → users, UNIQUE) |
| points_balance | INT |
| tier | VARCHAR (BRONZE, SILVER, GOLD, PLATINUM) |

### support_tickets / support_messages

See [Support Tickets](#support-tickets) section for field details.

### seller_details

See [POST /me/seller-details](#post-meseller-details) for field details.

