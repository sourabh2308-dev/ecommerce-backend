# Payment Service — API Documentation

**Base URL:** `http://localhost:8080/api/payment`  
**Internal Port:** 8080  
**Database:** payment_db (PostgreSQL)  
**Events:** Kafka (payment confirmation/failure events)  
**Payment Gateway:** Razorpay (production) / MockPaymentGateway (development)

---

## Table of Contents

- [Overview](#overview)
- [Payments](#payments)
  - [POST / (Initiate Payment)](#post--initiate-payment)
  - [GET /](#get-)
  - [GET /{uuid}](#get-uuid)
  - [GET /order/{orderUuid}](#get-orderorderuuid)
  - [GET /seller](#get-seller)
  - [POST /gateway/webhook](#post-gatewaywebhook)
- [Dashboards](#dashboards)
  - [GET /admin/dashboard](#get-admindashboard)
  - [GET /seller/dashboard](#get-sellerdashboard)
- [Internal Endpoints](#internal-endpoints)
- [Error Responses](#error-responses)
- [Database Schema](#database-schema)
- [Payment Gateway Configuration](#payment-gateway-configuration)

---

## Overview

The Payment Service handles payment processing via Razorpay integration (or a mock gateway in development). When a payment is confirmed, it publishes a Kafka event that the Order Service consumes to update order status. For multi-seller orders, payments are automatically split among sellers.

**Payment Flow:**
```
Order Placed
  ↓
Initiate Payment (creates internal payment + gateway order)
  ↓
Client-side Razorpay checkout
  ↓
Razorpay webhook callback (`/gateway/webhook`) → verify signature
  ↓
Payment confirmed → Kafka event → Order Service updates status to CONFIRMED
  ↓
Payment splits calculated per seller
```

**Payment Status Values:** `INITIATED`, `PENDING`, `SUCCESS`, `FAILED`

---

## Payments

---

### POST / (Initiate Payment)

Initiate a payment for an order. Creates a Razorpay order (or mock order in dev) and returns the payment ID and gateway details for client-side checkout.

**Auth Required:** Yes (BUYER only)

```http
POST /api/payment
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "orderUuid": "order-uuid-1",
  "amount": 71999.00
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| orderUuid | string | Yes | The order to pay for |
| amount | double | Yes | Payment amount |

**Response (200 OK):**

```json
"order_MockRzp123456"
```

The `gatewayOrderId` is passed to the Razorpay checkout SDK on the frontend.


---

### GET /

List the authenticated buyer's payments (paginated).

**Auth Required:** Yes (BUYER)

```http
GET /api/payment?page=0&size=10
```

| Parameter | Type | Required |
|-----------|------|:--------:|
| page | int | No |
| size | int | No |

**Response (200 OK):**

```json
[
  {
    "uuid": "pay-uuid-1",
    "orderUuid": "order-uuid-1",
    "amount": 71999.00,
    "status": "SUCCESS",
    "createdAt": "2026-03-06T12:00:00"
  }
]
```


---

### GET /{uuid}

Get payment details by UUID.

**Auth Required:** Yes

```http
GET /api/payment/pay-uuid-1
Authorization: Bearer eyJhbGci...
```

**Response (200 OK):**

```json
{
  "uuid": "pay-uuid-1",
  "orderUuid": "order-uuid-1",
  "buyerUuid": "user-uuid-1",
  "amount": 71999.00,
  "status": "SUCCESS",
  "splits": [
    {
      "sellerUuid": "seller-uuid-1",
      "sellerAmount": 65000.00,
      "platformFee": 4999.00,
      "deliveryFee": 2000.00
    }
  ],
  "createdAt": "2026-03-06T12:00:00"
}
```


---

### GET /order/{orderUuid}

Get payment for a specific order.

**Auth Required:** Yes

```http
GET /api/payment/order/order-uuid-1
```

**Response (200 OK):** `PaymentResponse`


---

### GET /seller

List payment splits for the authenticated seller.

**Auth Required:** Yes (SELLER only)

```http
GET /api/payment/seller?page=0&size=10
```

**Response (200 OK):**

```json
[
  {
    "uuid": "pay-uuid-1",
    "orderUuid": "order-uuid-1",
    "buyerUuid": "buyer-uuid-1",
    "amount": 71999.00,
    "status": "SUCCESS",
    "splits": [
      {
        "sellerUuid": "seller-uuid-1",
        "productUuid": "prod-uuid-1",
        "itemAmount": 71999.00,
        "platformFeePercent": 10.0,
        "platformFee": 7199.90,
        "deliveryFee": 30.0,
        "sellerPayout": 64769.10,
        "status": "COMPLETED"
      }
    ],
    "createdAt": "2026-03-06T12:00:00"
  }
]
```


---

### POST /gateway/webhook

Razorpay webhook callback endpoint. Called by Razorpay when a payment is completed, failed, or refunded.

**Auth Required:** No (verified by Razorpay signature)

```http
POST /api/payment/gateway/webhook
Content-Type: application/json
X-Razorpay-Signature: <hmac_sha256_signature>
```

```json
{
  "razorpay_order_id": "order_MockRzp123456",
  "razorpay_payment_id": "pay_MockRzp789012",
  "razorpay_signature": "mock_sig_abc123",
  "event": "payment.captured"
}
```

**Response (200 OK):**

Empty body.

**Failure responses:**
- `400 Bad Request` when `razorpay_order_id`, `razorpay_payment_id`, or signature is missing
- `401 Unauthorized` when signature verification fails

**Processing Flow:**
1. Resolve signature from header (`X-Razorpay-Signature`) or payload field (`razorpay_signature`)
2. Verify HMAC-SHA256 signature against the configured gateway secret
3. Derive callback success from event name (`failed`/`refund` => failure)
4. Update payment status to `SUCCESS` or `FAILED`
5. Publish `payment.completed` to Kafka for Order Service

**Idempotency:** Duplicate gateway callbacks are deduplicated using the `processed_events` table.


---

## Dashboards

---

### GET /admin/dashboard

Admin payment analytics.

**Auth Required:** Yes (ADMIN only)

```http
GET /api/payment/admin/dashboard
```

**Response (200 OK):**

```json
{
  "totalGrossRevenue": 15000000.00,
  "totalPlatformEarnings": 1500000.00,
  "totalDeliveryFees": 320000.00,
  "totalSellerPayouts": 13180000.00,
  "totalCompletedOrders": 1180,
  "activeSellers": 96
}
```


---

### GET /seller/dashboard

Seller payment analytics.

**Auth Required:** Yes (SELLER only)

```http
GET /api/payment/seller/dashboard
```

**Response (200 OK):**

```json
{
  "sellerUuid": "seller-uuid-1",
  "totalEarnings": 1200000.00,
  "pendingPayouts": 50000.00,
  "completedPayouts": 1150000.00,
  "totalOrders": 150
}
```


---

## Internal Endpoints

No internal REST endpoints are currently exposed.

Cross-service integration is handled asynchronously via Kafka events (`order.created`, `payment.completed`).

---

## Error Responses

```json
{
  "timestamp": "2026-03-06T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Payment already completed",
  "path": "/api/payment"
}
```

| Status | Common Causes |
|:------:|---------------|
| 400 | Invalid request, duplicate payment |
| 401 | Missing/invalid JWT |
| 403 | Wrong role |
| 404 | Payment/order not found |
| 409 | Payment already completed/refunded |
| 502 | Razorpay gateway error |

---

## Database Schema

### payments

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| order_uuid | VARCHAR | NOT NULL |
| buyer_uuid | VARCHAR | NOT NULL |
| amount | DOUBLE | NOT NULL |
| status | VARCHAR | INITIATED, PENDING, SUCCESS, FAILED |
| gateway_order_id | VARCHAR | Razorpay order ID |
| created_at | TIMESTAMP | |

### payment_splits

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| payment_id | BIGINT | FK → payments |
| seller_uuid | VARCHAR | NOT NULL |
| amount | DOUBLE | NOT NULL |
| status | VARCHAR | |
| created_at | TIMESTAMP | |

### processed_events

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | |
| event_id | VARCHAR (UNIQUE) | Unique processed callback/event key |
| topic | VARCHAR | Logical source topic/context (e.g. `payment.gateway.webhook`) |
| processed_at | TIMESTAMP | When the event was processed |

Used for idempotent webhook processing — prevents duplicate payment confirmations.

---

## Payment Gateway Configuration

### Production (Razorpay)

```yaml
razorpay:
  key-id: ${RAZORPAY_KEY_ID}
  key-secret: ${RAZORPAY_KEY_SECRET}
  webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}
```

### Development (Mock Gateway)

The `MockPaymentGateway` automatically creates mock Razorpay orders and simulates payment confirmations. No Razorpay account needed for local development.

Mock response format:
- Order ID: `order_MockRzp<timestamp>`
- Payment ID: `pay_MockRzp<timestamp>`
- Signature: `mock_signature_<hash>`

