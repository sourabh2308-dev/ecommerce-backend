# Application Flow

End-to-end flows for every major feature in the SourHub platform. Each flow traces the request from client through gateway, services, databases, and back.

---

## Table of Contents

- [1. User Registration & Email Verification](#1-user-registration--email-verification)
- [2. Login & JWT Authentication](#2-login--jwt-authentication)
- [3. Token Refresh & Rotation](#3-token-refresh--rotation)
- [4. Password Recovery (Forgot/Reset)](#4-password-recovery-forgotreset)
- [5. Seller Onboarding](#5-seller-onboarding)
- [6. Product Creation & Approval](#6-product-creation--approval)
- [7. Product Search & Browse](#7-product-search--browse)
- [8. Cart & Wishlist](#8-cart--wishlist)
- [9. Order Placement (Payment Saga)](#9-order-placement-payment-saga)
- [10. Multi-Seller Order Splitting](#10-multi-seller-order-splitting)
- [11. Payment Processing](#11-payment-processing)
- [12. Order Lifecycle](#12-order-lifecycle)
- [13. Coupon Validation & Application](#13-coupon-validation--application)
- [14. Return & Exchange Flow](#14-return--exchange-flow)
- [15. Shipment Tracking](#15-shipment-tracking)
- [16. Invoice Generation](#16-invoice-generation)
- [17. Review Submission](#17-review-submission)
- [18. Loyalty Points Flow](#18-loyalty-points-flow)
- [19. Support Ticket Flow](#19-support-ticket-flow)
- [20. Admin Operations](#20-admin-operations)

---

## 1. User Registration & Email Verification

```
Client                Gateway              User Service              Database
  │                     │                       │                       │
  │ POST /api/user/     │                       │                       │
  │   register          │                       │                       │
  │ ───────────────────►│                       │                       │
  │                     │  Forward (public)     │                       │
  │                     │ ─────────────────────►│                       │
  │                     │                       │ Validate input        │
  │                     │                       │ Check email unique    │
  │                     │                       │ Hash password (BCrypt)│
  │                     │                       │ Generate 6-digit OTP  │
  │                     │                       │                       │
  │                     │                       │ INSERT user           │
  │                     │                       │ (PENDING_VERIFICATION)│
  │                     │                       │ ─────────────────────►│
  │                     │                       │                       │
  │                     │                       │ INSERT otp_verification│
  │                     │                       │ (10 min expiry)       │
  │                     │                       │ ─────────────────────►│
  │                     │                       │                       │
  │                     │                       │ Send OTP email        │
  │                     │     201 Created       │                       │
  │ ◄───────────────────│◄─────────────────────│                       │
  │                     │                       │                       │
  │ POST /api/user/     │                       │                       │
  │   verify-otp        │                       │                       │
  │ ───────────────────►│ ─────────────────────►│                       │
  │                     │                       │ Validate OTP code     │
  │                     │                       │ Mark OTP as used      │
  │                     │                       │ Update user status:   │
  │                     │                       │  BUYER → ACTIVE       │
  │                     │                       │  SELLER → PENDING_    │
  │                     │                       │           DETAILS     │
  │                     │      200 OK           │                       │
  │ ◄───────────────────│◄─────────────────────│                       │
```

**Status transitions:**
- Registration: `→ PENDING_VERIFICATION`
- OTP verified (Buyer): `→ ACTIVE`
- OTP verified (Seller): `→ PENDING_DETAILS` (needs seller details submission)
- Seller details submitted: `→ PENDING_APPROVAL`
- Admin approves: `→ ACTIVE`

---

## 2. Login & JWT Authentication

```
Client              Gateway            Auth Service         User Service
  │                    │                     │                    │
  │ POST /api/auth/    │                     │                    │
  │   login            │                     │                    │
  │ ──────────────────►│                     │                    │
  │                    │  Forward (public)   │                    │
  │                    │ ──────────────────► │                    │
  │                    │                     │ GET /internal/     │
  │                    │                     │   email/{email}    │
  │                    │                     │ ──────────────────►│
  │                    │                     │                    │
  │                    │                     │   UserDto          │
  │                    │                     │ ◄──────────────────│
  │                    │                     │                    │
  │                    │                     │ Verify BCrypt hash │
  │                    │                     │ Generate Access    │
  │                    │                     │   Token (JWT,      │
  │                    │                     │   HS256, 15 min)   │
  │                    │                     │ Generate Refresh   │
  │                    │                     │   Token (UUID,     │
  │                    │                     │   7 days, DB)      │
  │                    │                     │                    │
  │                    │  AuthResponse       │                    │
  │                    │  {accessToken,      │                    │
  │                    │   refreshToken,     │                    │
  │                    │   tokenType}        │                    │
  │ ◄──────────────────│◄────────────────── │                    │
```

**JWT Claims:**
```json
{
  "sub": "user@example.com",
  "uuid": "user-uuid-here",
  "role": "BUYER",
  "iat": 1709721600,
  "exp": 1709722500
}
```

---

## 3. Token Refresh & Rotation

```
Client              Gateway            Auth Service            Database
  │                    │                     │                      │
  │ POST /api/auth/    │                     │                      │
  │   refresh?         │                     │                      │
  │   refreshToken=X   │                     │                      │
  │ ──────────────────►│ ──────────────────► │                      │
  │                    │                     │ Lookup token X in DB │
  │                    │                     │ ────────────────────►│
  │                    │                     │                      │
  │                    │                     │ Check:               │
  │                    │                     │  - Not expired       │
  │                    │                     │  - Not revoked       │
  │                    │                     │                      │
  │                    │                     │ REVOKE old token X   │
  │                    │                     │ ────────────────────►│
  │                    │                     │                      │
  │                    │                     │ Generate new access  │
  │                    │                     │   token + new        │
  │                    │                     │   refresh token Y    │
  │                    │                     │                      │
  │                    │                     │ INSERT token Y       │
  │                    │                     │ ────────────────────►│
  │                    │                     │                      │
  │  {accessToken,     │                     │                      │
  │   refreshToken: Y} │                     │                      │
  │ ◄──────────────────│◄────────────────── │                      │
```

**Key behavior:** Old refresh token is revoked on every refresh — each token can only be used once (rotation).

---

## 4. Password Recovery (Forgot/Reset)

```
Client              Gateway            Auth Service         User Service
  │                    │                     │                    │
  │ POST /api/auth/    │                     │                    │
  │   forgot-password  │                     │                    │
  │  {email}           │                     │                    │
  │ ──────────────────►│ ──────────────────► │                    │
  │                    │                     │ POST /internal/    │
  │                    │                     │   forgot-password  │
  │                    │                     │ ──────────────────►│
  │                    │                     │                    │ Generate OTP
  │                    │                     │                    │ (PASSWORD_RESET type)
  │                    │                     │                    │ Send email
  │                    │  "OTP sent"         │                    │
  │ ◄──────────────────│◄────────────────── │◄──────────────────│
  │                    │                     │                    │
  │ POST /api/auth/    │                     │                    │
  │   reset-password   │                     │                    │
  │  {email, otpCode,  │                     │                    │
  │   newPassword}     │                     │                    │
  │ ──────────────────►│ ──────────────────► │                    │
  │                    │                     │ POST /internal/    │
  │                    │                     │   reset-password   │
  │                    │                     │ ──────────────────►│
  │                    │                     │                    │ Validate OTP
  │                    │                     │                    │ Hash new password
  │                    │                     │                    │ Update user
  │                    │  "Password reset"   │                    │
  │ ◄──────────────────│◄────────────────── │◄──────────────────│
```

---

## 5. Seller Onboarding

```
1. Register with role=SELLER → Status: PENDING_VERIFICATION
2. Verify OTP              → Status: PENDING_DETAILS
3. POST /api/user/me/seller-details
   Submit: businessName, businessType, GST, PAN, bank details
                           → Status: PENDING_APPROVAL
4. Admin GET /api/user/admin/pending-sellers
   Admin PUT /api/user/admin/approve/{uuid}
                           → Status: ACTIVE (seller can now create products)
```

---

## 6. Product Creation & Approval

```
Seller                 Gateway              Product Service          Database
  │                       │                       │                     │
  │ POST /api/product     │                       │                     │
  │  {name, description,  │                       │                     │
  │   price, stock,       │                       │                     │
  │   category, imageUrl} │                       │                     │
  │ ─────────────────────►│                       │                     │
  │                       │ X-User-UUID: seller   │                     │
  │                       │ X-User-Role: SELLER   │                     │
  │                       │ ─────────────────────►│                     │
  │                       │                       │ Create product      │
  │                       │                       │ Status: DRAFT       │
  │                       │                       │ sellerUuid = header │
  │                       │                       │ ───────────────────►│
  │                       │  201 ProductResponse  │                     │
  │ ◄─────────────────────│◄─────────────────────│                     │
  │                       │                       │                     │

Admin                  Gateway              Product Service          Database
  │                       │                       │                     │
  │ PUT /api/product/     │                       │                     │
  │   admin/approve/{uuid}│                       │                     │
  │ ─────────────────────►│ ─────────────────────►│                     │
  │                       │                       │ Update status:      │
  │                       │                       │ DRAFT → ACTIVE      │
  │                       │                       │ ───────────────────►│
  │                       │                       │                     │
  │                       │                       │ Index in            │
  │                       │                       │ Elasticsearch       │
  │                       │                       │                     │
  │ ◄─────────────────────│◄─────────────────────│                     │
```

**Product lifecycle:** `DRAFT → ACTIVE → BLOCKED → ACTIVE` (admin can block/unblock)

---

## 7. Product Search & Browse

```
Client              Gateway            Product Service       Elasticsearch    Redis
  │                    │                     │                     │            │
  │ GET /api/product/  │                     │                     │            │
  │   search?q=laptop  │                     │                     │            │
  │   &minPrice=500    │                     │                     │            │
  │   &category=elec   │                     │                     │            │
  │ ──────────────────►│ ──────────────────► │                     │            │
  │                    │                     │ Check Redis cache   │            │
  │                    │                     │ ───────────────────────────────► │
  │                    │                     │                     │ MISS       │
  │                    │                     │ ◄──────────────────────────────  │
  │                    │                     │                     │            │
  │                    │                     │ Query Elasticsearch │            │
  │                    │                     │ ───────────────────►│            │
  │                    │                     │  Search results     │            │
  │                    │                     │ ◄───────────────────│            │
  │                    │                     │                     │            │
  │                    │                     │ Cache results       │            │
  │                    │                     │ ───────────────────────────────► │
  │                    │                     │                     │            │
  │  CursorPageResponse│                     │                     │            │
  │ ◄──────────────────│◄────────────────── │                     │            │
```

---

## 8. Cart & Wishlist

```
Buyer               Gateway              User Service          Redis Cache
  │                    │                       │                     │
  │ POST /api/user/    │                       │                     │
  │   cart             │                       │                     │
  │  {productUuid,     │                       │                     │
  │   productName,     │                       │                     │
  │   price, qty}      │                       │                     │
  │ ──────────────────►│ ─────────────────────►│                     │
  │                    │                       │ Check: existing     │
  │                    │                       │ item for same       │
  │                    │                       │ product?            │
  │                    │                       │  YES → increment    │
  │                    │                       │  NO → insert        │
  │                    │                       │                     │
  │                    │                       │ Evict cart cache     │
  │                    │                       │ ───────────────────►│
  │                    │                       │                     │
  │  CartResponse      │                       │                     │
  │  {items, totalItems,│                      │                     │
  │   totalAmount}     │                       │                     │
  │ ◄──────────────────│◄─────────────────────│                     │
```

**Note:** Cart/wishlist items store denormalized product data (name, image, price) to avoid cross-service calls on cart page loads.

---

## 9. Order Placement (Payment Saga)

This is the most complex flow — it involves Order Service, Payment Service, and Product Service coordinated via Kafka.

```
Buyer               Gateway         Order Service      Kafka        Payment Service     Product Service
  │                    │                 │                │                │                  │
  │ POST /api/order    │                 │                │                │                  │
  │ {items, shipping}  │                 │                │                │                  │
  │ ──────────────────►│ ──────────────► │                │                │                  │
  │                    │                 │                │                │                  │
  │                    │                 │ Validate items │                │                  │
  │                    │                 │ Calculate total│                │                  │
  │                    │                 │ Apply coupon   │                │                  │
  │                    │                 │                │                │                  │
  │                    │                 │ Reduce stock   │                │                  │
  │                    │                 │ ─────────────────────────────────────────────────► │
  │                    │                 │                │                │      200 OK      │
  │                    │                 │ ◄──────────────────────────────────────────────── │
  │                    │                 │                │                │                  │
  │                    │                 │ INSERT order   │                │                  │
  │                    │                 │ (CREATED,      │                │                  │
  │                    │                 │  PENDING pay)  │                │                  │
  │                    │                 │                │                │                  │
  │                    │                 │ Split into     │                │                  │
  │                    │                 │ sub-orders     │                │                  │
  │                    │                 │ (per seller)   │                │                  │
  │                    │                 │                │                │                  │
  │                    │                 │ Publish event  │                │                  │
  │                    │                 │  "order.created"                │                  │
  │                    │                 │ ──────────────►│                │                  │
  │                    │                 │                │                │                  │
  │  OrderResponse     │                 │                │ Consume event  │                  │
  │ ◄──────────────────│◄────────────── │                │ ──────────────►│                  │
  │                    │                 │                │                │                  │
  │                    │                 │                │                │ Initiate payment │
  │                    │                 │                │                │ (Razorpay/Mock)  │
  │                    │                 │                │                │                  │
  │                    │                 │                │  "payment.     │                  │
  │                    │                 │                │   completed"   │                  │
  │                    │                 │                │ ◄──────────────│                  │
  │                    │                 │                │                │                  │
  │                    │                 │ Consume event  │                │                  │
  │                    │                 │ ◄──────────────│                │                  │
  │                    │                 │                │                │                  │
  │                    │                 │ Update order:  │                │                  │
  │                    │                 │ CONFIRMED /    │                │                  │
  │                    │                 │ FAILED         │                │                  │
  │                    │                 │                │                │                  │
  │                    │                 │ If FAILED:     │                │                  │
  │                    │                 │  restore stock │                │                  │
  │                    │                 │ ─────────────────────────────────────────────────► │
```

**Compensating actions on failure:**
- Payment fails → Order status set to `CANCELLED`, payment status to `FAILED`
- Stock is restored via internal call to Product Service
- Coupon usage count is decremented

---

## 10. Multi-Seller Order Splitting

When an order contains items from multiple sellers, it is automatically split:

```
Original Order (MAIN):
  Item A → Seller 1 (₹500)
  Item B → Seller 2 (₹300)
  Item C → Seller 1 (₹200)

Resulting Orders:
  ┌── Main Order (uuid: main-uuid, type: MAIN, total: ₹1000)
  │
  ├── Sub-Order 1 (uuid: sub-1, type: SUB, parent: main-uuid, seller: Seller 1)
  │   ├── Item A (₹500)
  │   └── Item C (₹200)
  │   └── Total: ₹700
  │
  └── Sub-Order 2 (uuid: sub-2, type: SUB, parent: main-uuid, seller: Seller 2)
      └── Item B (₹300)
      └── Total: ₹300

All share the same orderGroupId for correlation.
```

---

## 11. Payment Processing

```
Payment Service                   Razorpay                      Order Service
     │                                │                              │
     │  Kafka: order.created          │                              │
     │  {orderUuid, amount, buyer}    │                              │
     │                                │                              │
     │  Create Payment record         │                              │
     │  (INITIATED)                   │                              │
     │                                │                              │
     │  Create Razorpay Order         │                              │
     │ ──────────────────────────────►│                              │
     │                                │                              │
     │  {razorpay_order_id}           │                              │
     │ ◄──────────────────────────────│                              │
     │                                │                              │
     │  Update Payment (PENDING)      │                              │
     │  Store gateway_order_id        │                              │
     │                                │                              │
     │  ... Client completes payment  │                              │
     │     on Razorpay checkout ...   │                              │
     │                                │                              │
     │  POST /webhook/razorpay        │                              │
     │ ◄──────────────────────────────│                              │
     │                                │                              │
     │  Verify webhook signature      │                              │
     │  Update Payment (SUCCESS)      │                              │
     │                                │                              │
     │  Create PaymentSplits          │                              │
     │  (per seller allocation)       │                              │
     │                                │                              │
     │  Kafka: payment.completed      │                              │
     │  {orderUuid, status: SUCCESS}  │                              │
     │ ──────────────────────────────────────────────────────────────►│
     │                                │                              │
     │                                │                   Update order:
     │                                │                   CONFIRMED,
     │                                │                   paymentStatus:
     │                                │                   SUCCESS
```

**Mock gateway:** In dev, `MockPaymentGateway` auto-succeeds all payments without Razorpay.

---

## 12. Order Lifecycle

```
                 ┌──────────┐
                 │ CREATED  │  ← Order placed, awaiting payment
                 └────┬─────┘
                      │ payment.completed (SUCCESS)
                 ┌────▼─────┐
                 │CONFIRMED │  ← Payment received
                 └────┬─────┘
                      │ seller/admin updates status
                 ┌────▼─────┐
                 │ SHIPPED  │  ← Goods dispatched
                 └────┬─────┘
                      │ delivery confirmed
                 ┌────▼──────┐
                 │ DELIVERED │  ← Buyer received goods
                 └────┬──────┘
                      │ buyer requests return
            ┌─────────▼──────────┐
            │ RETURN_REQUESTED   │
            └─────────┬──────────┘
                      │ admin approves
            ┌─────────▼──────────┐
            │ PICKUP_SCHEDULED   │
            └─────────┬──────────┘
                      │ carrier picks up
            ┌─────────▼──────────┐
            │    PICKED_UP       │
            └─────────┬──────────┘
                      │ warehouse receives
            ┌─────────▼──────────┐
            │ RETURN_RECEIVED    │
            └─────────┬──────────┘
               ┌──────┴──────┐
               ▼             ▼
        ┌────────────┐ ┌──────────────┐
        │REFUND_ISSUED│ │EXCHANGE_ISSUED│
        └────────────┘ └──────────────┘

Cancellation (any time before SHIPPED):
  CREATED / CONFIRMED → CANCELLED
  Stock restored, payment refunded
```

---

## 13. Coupon Validation & Application

```
Buyer                    Order Service                     Database
  │                           │                               │
  │ POST /api/order/coupons/  │                               │
  │   validate                │                               │
  │  {code, orderAmount}      │                               │
  │ ─────────────────────────►│                               │
  │                           │ Lookup coupon by code         │
  │                           │ ──────────────────────────────►│
  │                           │                               │
  │                           │ Validate:                     │
  │                           │  - isActive?                  │
  │                           │  - Not expired?               │
  │                           │  - Under usage limit?         │
  │                           │  - Meets min order amount?    │
  │                           │                               │
  │                           │ Calculate discount:           │
  │                           │  PERCENTAGE: min(amt * %,     │
  │                           │              maxDiscount)     │
  │                           │  FLAT: discountValue          │
  │                           │                               │
  │  {isValid, code,          │                               │
  │   discountAmount, message}│                               │
  │ ◄─────────────────────────│                               │
```

---

## 14. Return & Exchange Flow

```
Buyer                Order Service              Admin              Payment Service
  │                       │                       │                      │
  │ POST /api/order/      │                       │                      │
  │   returns             │                       │                      │
  │  {orderUuid,          │                       │                      │
  │   returnType: REFUND, │                       │                      │
  │   reason}             │                       │                      │
  │ ─────────────────────►│                       │                      │
  │                       │ Validate:             │                      │
  │                       │  Order is DELIVERED   │                      │
  │                       │  Within return window │                      │
  │                       │ Create ReturnRequest  │                      │
  │                       │ (PENDING)             │                      │
  │                       │ Update order:         │                      │
  │                       │ RETURN_REQUESTED      │                      │
  │ ◄─────────────────────│                       │                      │
  │                       │                       │                      │
  │                       │ PUT /returns/{id}/    │                      │
  │                       │   approve             │                      │
  │                       │ ◄─────────────────────│                      │
  │                       │                       │                      │
  │                       │ Update: APPROVED      │                      │
  │                       │ Schedule pickup       │                      │
  │                       │ ───────────────────── │                      │
  │                       │                       │                      │
  │  ... pickup, receive, │ inspect ...           │                      │
  │                       │                       │                      │
  │                       │ If REFUND:            │                      │
  │                       │  Process refund       │                      │
  │                       │ ─────────────────────────────────────────── ►│
  │                       │                       │                      │
  │                       │ Order: REFUND_ISSUED  │                      │
  │ ◄─────────────────────│                       │                      │
```

---

## 15. Shipment Tracking

```
Seller/Admin           Order Service              Buyer
     │                      │                       │
     │ POST /api/order/     │                       │
     │   tracking           │                       │
     │  {orderUuid,         │                       │
     │   status: SHIPPED,   │                       │
     │   carrier, tracking#}│                       │
     │ ────────────────────►│                       │
     │                      │ Insert tracking event │
     │                      │ Update order status   │
     │                      │ Create notification   │
     │ ◄────────────────────│                       │
     │                      │                       │
     │                      │ GET /tracking/{uuid}  │
     │                      │ ◄─────────────────────│
     │                      │                       │
     │                      │ [                     │
     │                      │  {status: SHIPPED,    │
     │                      │   location: Delhi,    │
     │                      │   time: ...},         │
     │                      │  {status: IN_TRANSIT, │
     │                      │   location: Mumbai,   │
     │                      │   time: ...},         │
     │                      │  {status: DELIVERED,  │
     │                      │   location: Pune,     │
     │                      │   time: ...}          │
     │                      │ ]                     │
     │                      │ ─────────────────────►│
```

---

## 16. Invoice Generation

```
Buyer                  Order Service                Email Service
  │                         │                            │
  │ GET /api/order/         │                            │
  │   {uuid}/invoice        │                            │
  │ ───────────────────────►│                            │
  │                         │ Generate PDF (iText):      │
  │                         │  - Company header          │
  │                         │  - Order details           │
  │                         │  - Item table              │
  │                         │  - Tax breakdown (18% GST) │
  │                         │  - Total amount            │
  │                         │  - Shipping address        │
  │                         │                            │
  │   PDF byte[]            │                            │
  │ ◄───────────────────────│                            │
  │                         │                            │
  │ GET /api/order/         │                            │
  │   {uuid}/invoice/email  │                            │
  │ ───────────────────────►│                            │
  │                         │ Generate PDF               │
  │                         │ Send as email attachment   │
  │                         │ ──────────────────────────►│
  │   "Invoice emailed"     │                            │
  │ ◄───────────────────────│                            │
```

**Auto-invoice:** When an order is marked DELIVERED, the system automatically generates and emails the invoice.

---

## 17. Review Submission

```
Buyer               Gateway           Review Service       Order Service     Product Service
  │                    │                    │                    │                  │
  │ POST /api/review   │                    │                    │                  │
  │ {orderUuid,        │                    │                    │                  │
  │  productUuid,      │                    │                    │                  │
  │  rating: 4,        │                    │                    │                  │
  │  comment: "Great"} │                    │                    │                  │
  │ ──────────────────►│ ──────────────────►│                    │                  │
  │                    │                    │                    │                  │
  │                    │                    │ Verify purchase:   │                  │
  │                    │                    │ GET /internal/     │                  │
  │                    │                    │  order/{orderUuid} │                  │
  │                    │                    │ ──────────────────►│                  │
  │                    │                    │                    │                  │
  │                    │                    │ Check:             │                  │
  │                    │                    │  - Order DELIVERED │                  │
  │                    │                    │  - Buyer owns order│                  │
  │                    │                    │  - Product in order│                  │
  │                    │                    │  - Not yet reviewed│                  │
  │                    │                    │                    │                  │
  │                    │                    │ INSERT review      │                  │
  │                    │                    │ (verifiedPurchase  │                  │
  │                    │                    │  = true)           │                  │
  │                    │                    │                    │                  │
  │                    │                    │ Update product     │                  │
  │                    │                    │ average rating:    │                  │
  │                    │                    │ PUT /internal/     │                  │
  │                    │                    │  {uuid}/rating     │                  │
  │                    │                    │ ─────────────────────────────────────►│
  │                    │                    │                    │                  │
  │  ReviewResponse    │                    │                    │                  │
  │ ◄──────────────────│◄──────────────────│                    │                  │
```

---

## 18. Loyalty Points Flow

```
Points Earning (automated on order delivery):
  Order DELIVERED → Award points based on order amount
  ₹100 spent = 10 points (configurable)
  Points added to user's loyalty_points balance

Tier System:
  0 - 999 points    → BRONZE
  1000 - 4999 points → SILVER
  5000 - 9999 points → GOLD
  10000+ points      → PLATINUM

Points Redemption:
  POST /api/user/loyalty/redeem
  {points: 500, orderUuid: "..."}
  → Deduct from balance
  → Apply as discount (1 point = ₹1)
  → Record LoyaltyTransaction (REDEEM)
```

---

## 19. Support Ticket Flow

```
User                  User Service              Admin
  │                        │                       │
  │ POST /api/user/support │                       │
  │ {subject, description, │                       │
  │  category: ORDER,      │                       │
  │  orderUuid}            │                       │
  │ ──────────────────────►│                       │
  │                        │ Create ticket (OPEN)  │
  │ ◄──────────────────────│                       │
  │                        │                       │
  │ POST /support/{uuid}/  │                       │
  │   message              │                       │
  │ {content: "My order..."}                       │
  │ ──────────────────────►│                       │
  │ ◄──────────────────────│                       │
  │                        │                       │
  │                        │ PUT /support/{uuid}/  │
  │                        │   assign              │
  │                        │ ◄─────────────────────│
  │                        │   (assigns to admin)  │
  │                        │ ─────────────────────►│
  │                        │                       │
  │                        │ POST /support/{uuid}/ │
  │                        │   message             │
  │                        │ ◄─────────────────────│
  │                        │ {content: "Looking    │
  │                        │  into it..."}         │
  │                        │                       │
  │                        │ PUT /support/{uuid}/  │
  │                        │   status              │
  │                        │ ◄─────────────────────│
  │                        │ (IN_PROGRESS→RESOLVED)│
  │                        │                       │
  │                        │ POST /support/{uuid}/ │
  │                        │   close               │
  │                        │ ◄─────────────────────│
  │                        │ Status: CLOSED        │
```

---

## 20. Admin Operations

### User Management
```
GET  /api/user/admin/users          → List all users (paginated, filterable)
GET  /api/user/admin/users/search   → Search by name/email
PUT  /api/user/admin/block/{uuid}   → Block user (ACTIVE → BLOCKED)
PUT  /api/user/admin/unblock/{uuid} → Unblock user (BLOCKED → ACTIVE)
PUT  /api/user/admin/approve/{uuid} → Approve seller (PENDING_APPROVAL → ACTIVE)
```

### Product Moderation
```
PUT  /api/product/admin/approve/{uuid} → Approve product (DRAFT → ACTIVE)
PUT  /api/product/admin/block/{uuid}   → Block product (hide from catalog)
PUT  /api/product/admin/unblock/{uuid} → Unblock product
```

### Order Management
```
GET  /api/order                     → List all orders
PUT  /api/order/{uuid}/status       → Update order status
GET  /api/order/audit               → View audit trail
GET  /api/order/dashboard/admin     → Platform KPIs
```

### Return Approvals
```
GET  /api/order/returns             → List all return requests
PUT  /api/order/returns/{id}/approve → Approve with refund amount
PUT  /api/order/returns/{id}/reject  → Reject with reason
```
