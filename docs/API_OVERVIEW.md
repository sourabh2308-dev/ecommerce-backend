# API Overview

Master reference of every endpoint across all services. Each row links to the detailed per-service documentation where you'll find request/response schemas, examples, and Postman screenshots.

> **Base URL:** `http://localhost:8080` (API Gateway)

---

## Table of Contents

- [Authentication](#authentication)
- [User Management](#user-management)
- [Addresses](#addresses)
- [Cart](#cart)
- [Wishlist](#wishlist)
- [Notifications](#notifications)
- [Loyalty Program](#loyalty-program)
- [Support Tickets](#support-tickets)
- [Products](#products)
- [Categories](#categories)
- [Product Images](#product-images)
- [Product Variants](#product-variants)
- [Flash Deals](#flash-deals)
- [Orders](#orders)
- [Coupons](#coupons)
- [Returns & Exchanges](#returns--exchanges)
- [Shipment Tracking](#shipment-tracking)
- [Invoices](#invoices)
- [Dashboards](#dashboards)
- [Audit Logs](#audit-logs)
- [Payments](#payments)
- [Reviews](#reviews)
- [Internal Endpoints](#internal-endpoints)

---

## Authentication

> Full details: [AUTH_SERVICE.md](services/AUTH_SERVICE.md)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/auth/login` | Public | Login with email & password |
| POST | `/api/auth/refresh` | Public | Rotate refresh token, get new access token |
| POST | `/api/auth/logout` | Public | Revoke refresh token |
| POST | `/api/auth/forgot-password` | Public | Send password reset OTP to email |
| POST | `/api/auth/reset-password` | Public | Reset password using OTP |

---

## User Management

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/user/register` | Public | Create new account |
| POST | `/api/user/verify-otp` | Public | Verify email with OTP |
| POST | `/api/user/resend-otp` | Public | Resend verification OTP |
| GET | `/api/user/me` | Authenticated | Get own profile |
| PUT | `/api/user/me` | Authenticated | Update profile |
| PUT | `/api/user/me/change-password` | Authenticated | Change password |
| POST | `/api/user/me/seller-details` | SELLER | Submit seller verification documents |

---

## Addresses

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md#addresses)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/user/me/addresses` | Authenticated | List all addresses |
| POST | `/api/user/me/addresses` | Authenticated | Add new address |
| PUT | `/api/user/me/addresses/{addressUuid}` | Authenticated | Update address |
| DELETE | `/api/user/me/addresses/{addressUuid}` | Authenticated | Delete address |
| PUT | `/api/user/me/addresses/{addressUuid}/default` | Authenticated | Set as default |

---

## Cart

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md#cart)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/user/cart` | BUYER | Get cart contents |
| POST | `/api/user/cart` | BUYER | Add item to cart |
| PUT | `/api/user/cart/{itemId}` | BUYER | Update item quantity |
| DELETE | `/api/user/cart/{itemId}` | BUYER | Remove item from cart |
| DELETE | `/api/user/cart` | BUYER | Clear entire cart |

---

## Wishlist

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md#wishlist)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/user/wishlist` | BUYER | List wishlist items |
| POST | `/api/user/wishlist` | BUYER | Add to wishlist |
| DELETE | `/api/user/wishlist/{productUuid}` | BUYER | Remove from wishlist |
| GET | `/api/user/wishlist/check/{productUuid}` | BUYER | Check if product is wishlisted |

---

## Notifications

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md#notifications)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/user/notifications` | Authenticated | List notifications (paginated) |
| GET | `/api/user/notifications/unread` | Authenticated | List unread only |
| GET | `/api/user/notifications/unread-count` | Authenticated | Get unread count |
| PUT | `/api/user/notifications/{uuid}/read` | Authenticated | Mark one as read |
| PUT | `/api/user/notifications/read-all` | Authenticated | Mark all as read |

---

## Loyalty Program

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md#loyalty-program)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/user/loyalty/balance` | BUYER | Get points balance |
| GET | `/api/user/loyalty/history` | BUYER | Transaction history |
| POST | `/api/user/loyalty/redeem` | BUYER | Redeem points for discount |
| POST | `/api/user/loyalty/award` | ADMIN | Award points to user |

---

## Support Tickets

> Full details: [USER_SERVICE.md](services/USER_SERVICE.md#support-tickets)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/user/support` | BUYER, SELLER | Create ticket |
| GET | `/api/user/support/{uuid}` | Authenticated | Get ticket details |
| GET | `/api/user/support/me` | BUYER, SELLER | My tickets |
| GET | `/api/user/support` | ADMIN | All tickets |
| GET | `/api/user/support/status/{status}` | ADMIN | Filter by status |
| POST | `/api/user/support/{uuid}/message` | Authenticated | Post message on ticket |
| PUT | `/api/user/support/{uuid}/status` | ADMIN | Update ticket status |
| PUT | `/api/user/support/{uuid}/assign` | ADMIN | Assign ticket to self |
| POST | `/api/user/support/{uuid}/close` | ADMIN | Close ticket |

---

## Products

> Full details: [PRODUCT_SERVICE.md](services/PRODUCT_SERVICE.md)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/product` | SELLER | Create product (DRAFT) |
| GET | `/api/product/{uuid}` | Public | Get product details |
| PUT | `/api/product/{uuid}` | SELLER, ADMIN | Update product |
| DELETE | `/api/product/{uuid}` | SELLER, ADMIN | Soft-delete product |
| GET | `/api/product/search` | Public | Full-text search (Elasticsearch) |
| GET | `/api/product/category/{categoryUuid}` | Public | List by category |
| PUT | `/api/product/admin/approve/{uuid}` | ADMIN | Approve product (DRAFT → ACTIVE) |
| PUT | `/api/product/admin/block/{uuid}` | ADMIN | Block product |
| PUT | `/api/product/admin/unblock/{uuid}` | ADMIN | Unblock product |

---

## Categories

> Full details: [PRODUCT_SERVICE.md](services/PRODUCT_SERVICE.md#categories)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/category` | ADMIN | Create category |
| PUT | `/api/category/{uuid}` | ADMIN | Update category |
| DELETE | `/api/category/{uuid}` | ADMIN | Delete category |
| GET | `/api/category/{uuid}` | Public | Get category |
| GET | `/api/category/root` | Public | List root categories |
| GET | `/api/category/{parentUuid}/children` | Public | List child categories |
| GET | `/api/category/hierarchy/all` | Public | Full category tree |
| POST | `/api/category/reorder` | ADMIN | Reorder sibling categories |

---

## Product Images

> Full details: [PRODUCT_SERVICE.md](services/PRODUCT_SERVICE.md#product-images)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/product/{productUuid}/images` | SELLER | Add image |
| GET | `/api/product/{productUuid}/images` | Public | List images |
| DELETE | `/api/product/{productUuid}/images/{imageId}` | SELLER | Remove image |

---

## Product Variants

> Full details: [PRODUCT_SERVICE.md](services/PRODUCT_SERVICE.md#product-variants)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/product/{productUuid}/variants` | SELLER | Add variant |
| GET | `/api/product/{productUuid}/variants` | Public | List variants |
| PUT | `/api/product/variants/{variantUuid}` | SELLER | Update variant |
| DELETE | `/api/product/variants/{variantUuid}` | SELLER | Delete variant |

---

## Flash Deals

> Full details: [PRODUCT_SERVICE.md](services/PRODUCT_SERVICE.md#flash-deals)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/product/deals` | SELLER | Create flash deal |
| GET | `/api/product/deals/active` | Public | List active deals |
| GET | `/api/product/deals/my` | SELLER | My deals |
| DELETE | `/api/product/deals/{dealUuid}` | SELLER | Cancel deal |

---

## Orders

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/order` | BUYER | Create order (triggers payment saga) |
| GET | `/api/order` | BUYER, ADMIN | List orders (filtered by role) |
| GET | `/api/order/{uuid}` | Authenticated | Get order details |
| PUT | `/api/order/{uuid}/status` | ADMIN, SELLER | Update order status |
| GET | `/api/order/seller` | SELLER | List seller's orders |
| GET | `/api/order/{uuid}/sub-orders` | Authenticated | Get sub-orders (multi-seller) |
| GET | `/api/order/group/{orderGroupId}` | Authenticated | Get all orders in group |

---

## Coupons

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md#coupons)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/order/coupons` | ADMIN, SELLER | Create coupon |
| POST | `/api/order/coupons/validate` | BUYER | Validate coupon code |
| GET | `/api/order/coupons` | ADMIN | List all coupons |
| DELETE | `/api/order/coupons/{code}` | ADMIN | Deactivate coupon |

---

## Returns & Exchanges

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md#returns--exchanges)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/order/returns` | BUYER | Request return/exchange |
| GET | `/api/order/returns/me` | BUYER | My return requests |
| GET | `/api/order/returns/{returnUuid}` | Authenticated | Get return details |
| PUT | `/api/order/returns/{returnUuid}/approve` | ADMIN | Approve return |
| PUT | `/api/order/returns/{returnUuid}/reject` | ADMIN | Reject return |
| PUT | `/api/order/returns/{returnUuid}/status` | ADMIN, SELLER | Update return fulfillment |
| GET | `/api/order/returns` | ADMIN | List all returns |

---

## Shipment Tracking

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md#shipment-tracking)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/order/tracking` | SELLER, ADMIN | Add tracking event |
| GET | `/api/order/tracking/{orderUuid}` | Authenticated | Get tracking history |

---

## Invoices

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md#invoices)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/order/{orderUuid}/invoice` | Authenticated | Download PDF invoice |
| GET | `/api/order/{orderUuid}/invoice/email` | Authenticated | Email invoice |

---

## Dashboards

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md#dashboards)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/order/dashboard/admin` | ADMIN | Platform KPIs |
| GET | `/api/order/dashboard/seller` | SELLER | Seller KPIs |
| GET | `/api/payment/admin/dashboard` | ADMIN | Admin payment metrics |
| GET | `/api/payment/seller/dashboard` | SELLER | Seller financial metrics |

---

## Audit Logs

> Full details: [ORDER_SERVICE.md](services/ORDER_SERVICE.md#audit-logs)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| GET | `/api/order/audit` | ADMIN | All audit logs |
| GET | `/api/order/audit/actor/{actorUuid}` | ADMIN | Logs by actor |
| GET | `/api/order/audit/resource/{resourceType}/{resourceId}` | ADMIN | Logs by resource |

---

## Payments

> Full details: [PAYMENT_SERVICE.md](services/PAYMENT_SERVICE.md)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/payment` | BUYER | Initiate payment |
| GET | `/api/payment` | BUYER | List my payments |
| GET | `/api/payment/{uuid}` | BUYER, ADMIN | Get payment details |
| GET | `/api/payment/order/{orderUuid}` | BUYER, ADMIN | Get payment by order |
| GET | `/api/payment/seller` | SELLER | Seller's payments |
| POST | `/api/payment/gateway/webhook` | Public (signed) | Razorpay webhook callback |

---

## Reviews

> Full details: [REVIEW_SERVICE.md](services/REVIEW_SERVICE.md)

| Method | Endpoint | Auth | Description |
|:------:|----------|:----:|-------------|
| POST | `/api/review` | BUYER | Submit review |
| GET | `/api/review/product/{productUuid}` | Public | List product reviews |
| GET | `/api/review/{uuid}` | Public | Get review details |
| PUT | `/api/review/{uuid}` | BUYER | Update own review |
| DELETE | `/api/review/{uuid}` | BUYER, ADMIN | Delete review |
| GET | `/api/review/me` | BUYER | My reviews |
| POST | `/api/review/{uuid}/vote` | Authenticated | Vote helpful/unhelpful |
| POST | `/api/review/{uuid}/images` | BUYER | Add review images |

---

## Internal Endpoints

These endpoints are not accessible through the API Gateway. They are called service-to-service using `X-Internal-Secret` for authentication.

| Method | Endpoint | From → To | Purpose |
|:------:|----------|-----------|---------|
| GET | `/api/user/internal/email/{email}` | Auth → User | Lookup user by email |
| GET | `/api/user/internal/uuid/{uuid}` | Auth → User | Lookup user by UUID |
| POST | `/api/user/internal/verify-otp` | Auth → User | Verify OTP for password reset |
| POST | `/api/user/internal/reset-password` | Auth → User | Execute password reset |
| POST | `/api/product/internal/{uuid}/reduce-stock` | Order → Product | Reduce stock after order |
| POST | `/api/product/internal/{uuid}/restore-stock` | Order → Product | Restore stock on cancellation |
| PUT | `/api/product/internal/{uuid}/update-rating` | Review → Product | Update average rating |
| POST | `/api/order/internal/payment-update` | Payment → Order | Payment result callback |
