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
  - [GET /my-payments](#get-my-payments)
  - [GET /{uuid}](#get-uuid)
  - [GET /order/{orderUuid}](#get-orderorderuuid)
  - [GET /seller/my-payments](#get-sellermy-payments)
  - [POST /webhook](#post-webhook)
- [Dashboards](#dashboards)
  - [GET /dashboard/admin](#get-dashboardadmin)
  - [GET /dashboard/seller](#get-dashboardseller)
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
Initiate Payment (creates Razorpay order)
  ↓
Client-side Razorpay checkout
  ↓
Razorpay webhook callback → verify signature
  ↓
Payment confirmed → Kafka event → Order Service updates status to CONFIRMED
  ↓
Payment splits calculated per seller
```

**Payment Status Values:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `REFUNDED`

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
  "amount": 71999.00,
  "currency": "INR",
  "paymentMethod": "RAZORPAY"
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| orderUuid | string | Yes | The order to pay for |
| amount | double | Yes | Payment amount |
| currency | string | No | Default: `INR` |
| paymentMethod | string | Yes | `RAZORPAY`, `COD` |

**Response (201 Created):**

```json
{
  "uuid": "pay-uuid-1",
  "orderUuid": "order-uuid-1",
  "amount": 71999.00,
  "currency": "INR",
  "status": "PENDING",
  "paymentMethod": "RAZORPAY",
  "gatewayOrderId": "order_MockRzp123456",
  "gatewayPaymentId": null,
  "createdAt": "2026-03-06T12:00:00"
}
```

The `gatewayOrderId` is passed to the Razorpay checkout SDK on the frontend.

<!-- 📸 Postman Screenshot: ![Initiate Payment](../screenshots/payment_initiate.png) -->

---

### GET /my-payments

List the authenticated buyer's payments (paginated).

**Auth Required:** Yes (BUYER)

```http
GET /api/payment/my-payments?page=0&size=10
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
    "status": "COMPLETED",
    "paymentMethod": "RAZORPAY",
    "createdAt": "2026-03-06T12:00:00"
  }
]
```

<!-- 📸 Postman Screenshot: ![My Payments](../screenshots/payment_my_payments.png) -->

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
  "userUuid": "user-uuid-1",
  "amount": 71999.00,
  "currency": "INR",
  "status": "COMPLETED",
  "paymentMethod": "RAZORPAY",
  "gatewayOrderId": "order_MockRzp123456",
  "gatewayPaymentId": "pay_MockRzp789012",
  "gatewaySignature": "mock_sig_abc123",
  "splits": [
    {
      "sellerUuid": "seller-uuid-1",
      "amount": 71999.00,
      "status": "COMPLETED"
    }
  ],
  "createdAt": "2026-03-06T12:00:00",
  "updatedAt": "2026-03-06T12:01:00"
}
```

<!-- 📸 Postman Screenshot: ![Get Payment](../screenshots/payment_get.png) -->

---

### GET /order/{orderUuid}

Get payment for a specific order.

**Auth Required:** Yes

```http
GET /api/payment/order/order-uuid-1
```

**Response (200 OK):** `PaymentResponse`

<!-- 📸 Postman Screenshot: ![Payment by Order](../screenshots/payment_by_order.png) -->

---

### GET /seller/my-payments

List payment splits for the authenticated seller.

**Auth Required:** Yes (SELLER only)

```http
GET /api/payment/seller/my-payments?page=0&size=10
```

**Response (200 OK):**

```json
[
  {
    "paymentUuid": "pay-uuid-1",
    "orderUuid": "order-uuid-1",
    "sellerUuid": "seller-uuid-1",
    "amount": 71999.00,
    "status": "COMPLETED",
    "createdAt": "2026-03-06T12:00:00"
  }
]
```

<!-- 📸 Postman Screenshot: ![Seller Payments](../screenshots/payment_seller.png) -->

---

### POST /webhook

Razorpay webhook callback endpoint. Called by Razorpay when a payment is completed, failed, or refunded.

**Auth Required:** No (verified by Razorpay signature)

```http
POST /api/payment/webhook
Content-Type: application/json
X-Razorpay-Signature: <hmac_sha256_signature>
```

```json
{
  "razorpay_order_id": "order_MockRzp123456",
  "razorpay_payment_id": "pay_MockRzp789012",
  "razorpay_signature": "mock_sig_abc123"
}
```

**Response (200 OK):**

```json
{
  "status": "success"
}
```

**Processing Flow:**
1. Verify HMAC-SHA256 signature against Razorpay webhook secret
2. Look up payment by `gatewayOrderId`
3. Update payment status to `COMPLETED`
4. Calculate seller splits for multi-seller orders
5. Publish `payment-confirmation` event to Kafka
6. Order Service consumes event and updates order to `CONFIRMED`

**Idempotency:** Webhook events are deduplicated using the `ProcessedEvent` table. Duplicate webhook calls are safely ignored.

<!-- 📸 Postman Screenshot: ![Webhook](../screenshots/payment_webhook.png) -->

---

## Dashboards

---

### GET /dashboard/admin

Admin payment analytics.

**Auth Required:** Yes (ADMIN only)

```http
GET /api/payment/dashboard/admin
```

**Response (200 OK):**

```json
{
  "totalPayments": 1250,
  "totalRevenue": 15000000.00,
  "completedPayments": 1180,
  "failedPayments": 45,
  "refundedPayments": 25,
  "pendingPayments": 0,
  "revenueByMonth": [
    { "month": "2026-01", "revenue": 4500000.00 },
    { "month": "2026-02", "revenue": 5200000.00 },
    { "month": "2026-03", "revenue": 5300000.00 }
  ]
}
```

<!-- 📸 Postman Screenshot: ![Admin Dashboard](../screenshots/payment_dashboard_admin.png) -->

---

### GET /dashboard/seller

Seller payment analytics.

**Auth Required:** Yes (SELLER only)

```http
GET /api/payment/dashboard/seller
```

**Response (200 OK):**

```json
{
  "totalEarnings": 1200000.00,
  "completedPayments": 150,
  "pendingPayments": 5,
  "averageOrderValue": 8000.00,
  "earningsByMonth": [...]
}
```

<!-- 📸 Postman Screenshot: ![Seller Dashboard](../screenshots/payment_dashboard_seller.png) -->

---

## Internal Endpoints

Secured by `X-Internal-Secret` header. Called by other services, not routed through API Gateway.

| Method | Endpoint | Called By | Purpose |
|:------:|----------|-----------|---------|
| POST | `/internal/split` | Order Service | Create payment splits for multi-seller orders |
| POST | `/internal/refund/{paymentUuid}` | Order Service | Initiate payment refund on cancellation |

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
| user_uuid | VARCHAR | NOT NULL |
| amount | DOUBLE | NOT NULL |
| currency | VARCHAR | Default: INR |
| status | VARCHAR | PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED |
| payment_method | VARCHAR | |
| gateway_order_id | VARCHAR | Razorpay order ID |
| gateway_payment_id | VARCHAR | Razorpay payment ID |
| gateway_signature | VARCHAR | HMAC signature |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

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
| event_id | VARCHAR (UNIQUE) | Razorpay webhook event ID |
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
