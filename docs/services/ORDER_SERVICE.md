# Order Service — API Documentation

**Base URL:** `http://localhost:8080/api/order`  
**Internal Port:** 8080  
**Database:** order_db (PostgreSQL)  
**Events:** Kafka (order events, payment events)  
**Dependencies:** Product Service (Feign), Payment Service (Kafka)

---

## Table of Contents

- [Overview](#overview)
- [Orders](#orders)
  - [POST / (Place Order)](#post--place-order)
  - [GET / (List Orders)](#get--list-orders)
  - [GET /seller (Seller Orders)](#get-seller-seller-orders)
  - [GET /{uuid}](#get-uuid)
  - [PUT /{uuid}/status](#put-uuidstatus)
  - [GET /{uuid}/sub-orders](#get-uuidsub-orders)
  - [GET /group/{groupId}](#get-groupgroupid)
- [Coupons](#coupons)
  - [POST /coupon](#post-coupon)
  - [GET /coupon/{code}](#get-couponcode)
  - [POST /coupon/validate](#post-couponvalidate)
  - [DELETE /coupon/{uuid}](#delete-couponuuid)
- [Returns](#returns)
  - [POST /return](#post-return)
  - [GET /return/{uuid}](#get-returnuuid)
  - [GET /return/my-returns](#get-returnmy-returns)
  - [GET /return/order/{orderUuid}](#get-returnorderorderuuid)
  - [PUT /return/admin/{uuid}/approve](#put-returnadminuuidapprove)
  - [PUT /return/admin/{uuid}/reject](#put-returnadminuuidreject)
  - [GET /return/admin/all](#get-returnadminall)
- [Shipment Tracking](#shipment-tracking)
  - [POST /tracking/{orderUuid}](#post-trackingorderuuid)
  - [GET /tracking/{orderUuid}](#get-trackingorderuuid)
- [Invoices](#invoices)
  - [GET /invoice/{orderUuid}](#get-invoiceorderuuid)
  - [GET /invoice/{orderUuid}/download](#get-invoiceorderuuiddownload)
  - [POST /invoice/{orderUuid}/generate](#post-invoiceorderuuidgenerate)
- [Dashboards](#dashboards)
  - [GET /dashboard/admin](#get-dashboardadmin)
  - [GET /dashboard/seller](#get-dashboardseller)
- [Audit Logs](#audit-logs)
  - [GET /audit/{orderUuid}](#get-auditorderuuid)
  - [GET /audit/admin/all](#get-auditadminall)
  - [GET /audit/admin/action/{action}](#get-auditadminactionaction)
- [Internal Endpoints](#internal-endpoints)
- [Error Responses](#error-responses)
- [Database Schema](#database-schema)

---

## Overview

The Order Service handles the complete order lifecycle including placement, multi-seller splitting, payment saga, fulfillment tracking, coupons, returns, invoices, and analytics dashboards.

**Order Status Lifecycle:**
```
CREATED → CONFIRMED → SHIPPED → DELIVERED
  └──→ CANCELLED
DELIVERED → RETURN_REQUESTED → PICKUP_SCHEDULED → PICKED_UP → RETURN_RECEIVED
RETURN_RECEIVED → REFUND_ISSUED / EXCHANGE_ISSUED
```

**Order Status Enum Values:**
`CREATED`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `PICKUP_SCHEDULED`, `PICKED_UP`, `RETURN_RECEIVED`, `EXCHANGE_ISSUED`, `REFUND_ISSUED`, `RETURN_REJECTED`

**Multi-Seller Order Splitting:**  
When a cart has items from multiple sellers, the order is split into separate sub-orders per seller. Each sub-order has its own lifecycle and payment split.

**Reliable Event Publication (Outbox):**  
Order creation writes an `order_event_outbox` row in the same transaction as order data, then publishes `order.created` after commit. Unpublished rows are retried by a scheduler until delivery succeeds.

---

## Orders

---

### POST / (Place Order)

Place a new order from the user's cart.

**Auth Required:** Yes (BUYER only)

```http
POST /api/order
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "items": [
    {
      "productUuid": "prod-uuid-1",
      "quantity": 1
    }
  ],
  "shippingName": "John Doe",
  "shippingAddress": "123 Main St",
  "shippingCity": "Mumbai",
  "shippingState": "MH",
  "shippingPincode": "400001",
  "shippingPhone": "+91-9999999999"
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| items | array | Yes | At least one `{productUuid, quantity}` entry |
| shippingName | string | Yes | Delivery recipient name |
| shippingAddress | string | Yes | Delivery street address |
| shippingCity | string | Yes | Delivery city |
| shippingState | string | Yes | Delivery state |
| shippingPincode | string | Yes | Delivery postal/PIN code |
| shippingPhone | string | Yes | Delivery contact number |

**Response (200 OK):**

```json
{
  "uuid": "order-uuid-1",
  "buyerUuid": "buyer-uuid-1",
  "items": [
    {
      "productUuid": "prod-uuid-1",
      "productName": "Laptop Pro X",
      "productCategory": "Electronics",
      "productImageUrl": "https://cdn.example.com/p1.jpg",
      "sellerUuid": "seller-uuid-1",
      "quantity": 1,
      "price": 79999.00
    }
  ],
  "totalAmount": 79999.00,
  "status": "CREATED",
  "paymentStatus": "PENDING",
  "orderType": "MAIN",
  "parentOrderUuid": null,
  "orderGroupId": "grp-uuid-1",
  "shippingName": "John Doe",
  "shippingAddress": "123 Main St",
  "shippingCity": "Mumbai",
  "shippingState": "MH",
  "shippingPincode": "400001",
  "shippingPhone": "+91-9999999999",
  "returnType": null,
  "returnReason": null,
  "createdAt": "2026-03-06T12:00:00",
  "updatedAt": "2026-03-06T12:00:00"
}
```

**Multi-Seller Response:** If the cart has items from multiple sellers, the response includes a parent order with `subOrders[]` containing per-seller sub-orders.


---

### GET /{uuid}

Get order details by UUID.

**Auth Required:** Yes

```http
GET /api/order/order-uuid-1
Authorization: Bearer eyJhbGci...
```

**Response (200 OK):** `OrderResponse`

| Status | Condition |
|:------:|-----------|
| 200 | Order found and accessible |
| 403 | Not order owner/admin/seller |
| 404 | Order not found |


---

### GET / (List Orders)

List orders visible to the caller.

- `BUYER`: own orders only
- `ADMIN`: all orders

**Auth Required:** Yes (BUYER or ADMIN)

```http
GET /api/order?page=0&size=10
```

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| page | int | No | Default: 0 |
| size | int | No | Default: 10 |

**Response (200 OK):** Paginated `OrderResponse` list


---

### GET /seller (Seller Orders)

List orders containing items sold by the authenticated seller.

**Auth Required:** Yes (SELLER)

```http
GET /api/order/seller?page=0&size=10
```

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| page | int | No | Default: 0 |
| size | int | No | Default: 10 |

**Response (200 OK):** Paginated `OrderResponse` list


---

### PUT /{uuid}/status

Update order status with role-based transition rules handled by service logic.

**Auth Required:** Yes (BUYER, SELLER, ADMIN)

```http
PUT /api/order/order-uuid-1/status?status=CONFIRMED
Authorization: Bearer eyJhbGci...
```

Optional query params when creating return request transitions:

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| status | string | Yes | Target order status |
| returnType | string | No | `REFUND` or `EXCHANGE` |
| returnReason | string | No | Free-text reason |

**Response (200 OK):** Updated `OrderResponse`


---

### GET /{uuid}/sub-orders

Fetch sub-orders for a split parent order.

**Auth Required:** Yes

```http
GET /api/order/order-uuid-1/sub-orders
```

**Response (200 OK):** `OrderResponse[]`


---

### GET /group/{groupId}

Fetch all orders in the same order group (parent + sub-orders).

**Auth Required:** Yes

```http
GET /api/order/group/grp-uuid-1
```

**Response (200 OK):** `OrderResponse[]`


---

## Coupons

**Base Path:** `/api/order/coupon`

---

### POST /coupon

Create a new coupon.

**Auth Required:** Yes (ADMIN or SELLER)

```http
POST /api/order/coupon
Content-Type: application/json
```

```json
{
  "code": "SAVE10",
  "discountPercent": 10.0,
  "maxDiscount": 5000.00,
  "minOrderAmount": 1000.00,
  "expiryDate": "2026-12-31T23:59:59",
  "usageLimit": 100,
  "isActive": true
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| code | string | Yes | Unique, alphanumeric |
| discountPercent | double | Yes | 1.0–100.0 |
| maxDiscount | double | No | Maximum discount cap |
| minOrderAmount | double | No | Minimum order value |
| expiryDate | datetime | Yes | Must be in future |
| usageLimit | int | No | Max total uses |
| isActive | boolean | No | Default: true |

**Response (201 Created):**

```json
{
  "uuid": "coupon-uuid-1",
  "code": "SAVE10",
  "discountPercent": 10.0,
  "maxDiscount": 5000.00,
  "minOrderAmount": 1000.00,
  "usageLimit": 100,
  "usedCount": 0,
  "expiryDate": "2026-12-31T23:59:59",
  "isActive": true
}
```


---

### GET /coupon/{code}

Get coupon details by code.

**Auth Required:** Yes

```http
GET /api/order/coupon/SAVE10
```


---

### POST /coupon/validate

Validate a coupon against an order amount.

**Auth Required:** Yes

```http
POST /api/order/coupon/validate
Content-Type: application/json
```

```json
{
  "code": "SAVE10",
  "orderAmount": 50000.00
}
```

**Response (200 OK):**

```json
{
  "valid": true,
  "discountAmount": 5000.00,
  "finalAmount": 45000.00,
  "message": "Coupon applied successfully"
}
```

Validation checks:
- Coupon exists and is active
- Not expired
- Usage limit not exceeded
- Order amount meets minimum


---

### DELETE /coupon/{uuid}

Deactivate a coupon.

**Auth Required:** Yes (ADMIN)

```http
DELETE /api/order/coupon/coupon-uuid-1
```


---

## Returns

**Base Path:** `/api/order/return`

---

### POST /return

Request a return or exchange.

**Auth Required:** Yes (BUYER)

```http
POST /api/order/return
Content-Type: application/json
```

```json
{
  "orderUuid": "order-uuid-1",
  "reason": "Product defective",
  "returnType": "RETURN",
  "items": [
    {
      "productUuid": "prod-uuid-1",
      "quantity": 1
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| orderUuid | string | Yes | Order UUID |
| reason | string | Yes | Return reason |
| returnType | string | Yes | `RETURN` or `EXCHANGE` |
| items | array | Yes | Items to return |

**Return Status Lifecycle:**
```
PENDING → APPROVED → PICKUP_SCHEDULED → PICKED_UP → RECEIVED → REFUNDED
  → REJECTED
  → EXCHANGED
```

**Response (201 Created):**

```json
{
  "uuid": "return-uuid-1",
  "orderUuid": "order-uuid-1",
  "reason": "Product defective",
  "returnType": "RETURN",
  "status": "PENDING",
  "items": [...],
  "createdAt": "2026-03-06T12:00:00"
}
```


---

### GET /return/{uuid}

Get return request details.

**Auth Required:** Yes

```http
GET /api/order/return/return-uuid-1
```


---

### GET /return/my-returns

List the buyer's return requests.

**Auth Required:** Yes (BUYER)

```http
GET /api/order/return/my-returns
```


---

### GET /return/order/{orderUuid}

List returns for a specific order.

**Auth Required:** Yes

```http
GET /api/order/return/order/order-uuid-1
```


---

### PUT /return/admin/{uuid}/approve

Approve a return request.

**Auth Required:** Yes (ADMIN)

```http
PUT /api/order/return/admin/return-uuid-1/approve
```

Side effects: Updates order status to `RETURNED` or `EXCHANGED`, triggers refund.


---

### PUT /return/admin/{uuid}/reject

Reject a return request.

**Auth Required:** Yes (ADMIN)

```http
PUT /api/order/return/admin/return-uuid-1/reject
```

```json
{
  "reason": "Return window expired"
}
```


---

### GET /return/admin/all

List all return requests (admin).

**Auth Required:** Yes (ADMIN)

```http
GET /api/order/return/admin/all?page=0&size=20
```


---

## Shipment Tracking

**Base Path:** `/api/order/tracking`

---

### POST /tracking/{orderUuid}

Add a tracking event to an order.

**Auth Required:** Yes (ADMIN or SELLER)

```http
POST /api/order/tracking/order-uuid-1
Content-Type: application/json
```

```json
{
  "status": "SHIPPED",
  "location": "Mumbai Sorting Hub",
  "description": "Package dispatched from warehouse",
  "carrier": "BlueDart",
  "trackingNumber": "BD123456789"
}
```

| Field | Type | Required |
|-------|------|:--------:|
| status | string | Yes |
| location | string | No |
| description | string | No |
| carrier | string | No |
| trackingNumber | string | No |

**Response (201 Created):**

```json
{
  "id": 1,
  "orderUuid": "order-uuid-1",
  "status": "SHIPPED",
  "location": "Mumbai Sorting Hub",
  "description": "Package dispatched from warehouse",
  "carrier": "BlueDart",
  "trackingNumber": "BD123456789",
  "timestamp": "2026-03-07T10:30:00"
}
```


---

### GET /tracking/{orderUuid}

Get all tracking events for an order (chronological).

**Auth Required:** Yes

```http
GET /api/order/tracking/order-uuid-1
```

**Response (200 OK):** Array of tracking events sorted by timestamp.


---

## Invoices

**Base Path:** `/api/order/invoice`

---

### GET /invoice/{orderUuid}

Get invoice details for an order.

**Auth Required:** Yes

```http
GET /api/order/invoice/order-uuid-1
```

**Response (200 OK):**

```json
{
  "invoiceNumber": "INV-2026-00001",
  "orderUuid": "order-uuid-1",
  "buyerName": "John Doe",
  "buyerEmail": "john@example.com",
  "items": [...],
  "subTotal": 79999.00,
  "discount": 8000.00,
  "tax": 12960.00,
  "totalAmount": 84959.00,
  "generatedAt": "2026-03-06T12:00:00"
}
```


---

### GET /invoice/{orderUuid}/download

Download invoice as PDF.

**Auth Required:** Yes

```http
GET /api/order/invoice/order-uuid-1/download
```

**Response:** Binary PDF file (`application/pdf`)


---

### POST /invoice/{orderUuid}/generate

Generate or regenerate an invoice for an order.

**Auth Required:** Yes (ADMIN)

```http
POST /api/order/invoice/order-uuid-1/generate
```


---

## Dashboards

---

### GET /dashboard/admin

Admin analytics dashboard.

**Auth Required:** Yes (ADMIN only)

```http
GET /api/order/dashboard/admin
```

**Response (200 OK):**

```json
{
  "totalOrders": 1250,
  "totalRevenue": 15000000.00,
  "pendingOrders": 45,
  "cancelledOrders": 80,
  "returnRequests": 25,
  "activeUsers": 500,
  "topSellingProducts": [...],
  "revenueByMonth": [...]
}
```


---

### GET /dashboard/seller

Seller analytics dashboard.

**Auth Required:** Yes (SELLER only)

```http
GET /api/order/dashboard/seller
```

**Response (200 OK):**

```json
{
  "totalOrders": 150,
  "totalRevenue": 1200000.00,
  "pendingOrders": 8,
  "returnRequests": 3,
  "averageOrderValue": 8000.00,
  "topProducts": [...]
}
```


---

## Audit Logs

**Base Path:** `/api/order/audit`

Every order status change and significant action is recorded.

---

### GET /audit/{orderUuid}

Get audit trail for a specific order.

**Auth Required:** Yes

```http
GET /api/order/audit/order-uuid-1
```

**Response (200 OK):**

```json
[
  {
    "id": 1,
    "orderUuid": "order-uuid-1",
    "action": "STATUS_CHANGED",
    "oldValue": "PENDING",
    "newValue": "CONFIRMED",
    "performedBy": "system",
    "timestamp": "2026-03-06T12:00:00"
  }
]
```


---

### GET /audit/admin/all

List all audit logs (paginated).

**Auth Required:** Yes (ADMIN)

```http
GET /api/order/audit/admin/all?page=0&size=50
```


---

### GET /audit/admin/action/{action}

Filter audit logs by action type.

**Auth Required:** Yes (ADMIN)

```http
GET /api/order/audit/admin/action/STATUS_CHANGED
```

Action types: `STATUS_CHANGED`, `ORDER_CREATED`, `ORDER_CANCELLED`, `RETURN_REQUESTED`, `PAYMENT_RECEIVED`, `COUPON_APPLIED`


---

## Internal Endpoints

Secured by `X-Internal-Secret` header. Not routed through API Gateway.

| Method | Endpoint | Called By | Purpose |
|:------:|----------|-----------|---------|
| POST | `/internal/payment-confirmation` | Payment Service (Kafka) | Confirm payment received |
| POST | `/internal/payment-failed` | Payment Service (Kafka) | Handle payment failure |

---

## Error Responses

```json
{
  "timestamp": "2026-03-06T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid coupon code",
  "path": "/api/order/coupon/validate"
}
```

| Status | Common Causes |
|:------:|---------------|
| 400 | Validation failure, invalid status transition, empty cart |
| 401 | Missing/invalid JWT |
| 403 | Not order owner, wrong role |
| 404 | Order/coupon/return not found |
| 409 | Coupon already used, return already submitted |
| 422 | Insufficient stock |

---

## Database Schema

### orders

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| user_uuid | VARCHAR | NOT NULL |
| total_amount | DOUBLE | NOT NULL |
| discount_amount | DOUBLE | |
| final_amount | DOUBLE | NOT NULL |
| coupon_code | VARCHAR | |
| status | VARCHAR | NOT NULL |
| payment_status | VARCHAR | |
| payment_method | VARCHAR | |
| order_type | VARCHAR | SINGLE_SELLER, MULTI_SELLER |
| parent_order_id | BIGINT | FK → orders (nullable) |
| shipping_address_uuid | VARCHAR | |
| billing_address_uuid | VARCHAR | |
| notes | VARCHAR | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### order_items

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| order_id | BIGINT (FK → orders) |
| product_uuid | VARCHAR |
| product_name | VARCHAR |
| quantity | INT |
| price | DOUBLE |
| seller_uuid | VARCHAR |

### coupons

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| code | VARCHAR (UNIQUE) |
| discount_percent | DOUBLE |
| max_discount | DOUBLE |
| min_order_amount | DOUBLE |
| usage_limit | INT |
| used_count | INT |
| expiry_date | TIMESTAMP |
| is_active | BOOLEAN |
| created_by | VARCHAR |

### return_requests

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| order_id | BIGINT (FK → orders) |
| reason | VARCHAR |
| return_type | VARCHAR (RETURN, EXCHANGE) |
| status | VARCHAR |
| admin_notes | VARCHAR |
| created_at | TIMESTAMP |

### shipment_tracking

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| order_id | BIGINT (FK → orders) |
| status | VARCHAR |
| location | VARCHAR |
| description | VARCHAR |
| carrier | VARCHAR |
| tracking_number | VARCHAR |
| timestamp | TIMESTAMP |

### invoices

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| order_id | BIGINT (FK → orders) |
| invoice_number | VARCHAR (UNIQUE) |
| pdf_url | VARCHAR |
| generated_at | TIMESTAMP |

### audit_logs

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| order_uuid | VARCHAR |
| action | VARCHAR |
| old_value | VARCHAR |
| new_value | VARCHAR |
| performed_by | VARCHAR |
| timestamp | TIMESTAMP |

### order_event_outbox

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| event_id | VARCHAR (UNIQUE) |
| order_uuid | VARCHAR (UNIQUE) |
| topic | VARCHAR |
| published | BOOLEAN |
| attempt_count | INT |
| created_at | TIMESTAMP |
| last_attempt_at | TIMESTAMP |
| published_at | TIMESTAMP |

