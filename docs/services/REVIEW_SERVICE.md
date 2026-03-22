# Review Service — API Documentation

**Base URL:** `http://localhost:8080/api/review`  
**Internal Port:** 8080  
**Database:** review_db (PostgreSQL)  
**Dependencies:** Product Service (Feign — updates average rating), Order Service (verifies purchase)

---

## Table of Contents

- [Overview](#overview)
- [Reviews](#reviews)
  - [POST / (Create Review)](#post--create-review)
  - [PUT /{uuid}](#put-uuid)
  - [DELETE /{uuid}](#delete-uuid)
  - [GET /product/{productUuid}](#get-productproductuuid)
  - [GET /my-reviews](#get-my-reviews)
- [Votes](#votes)
  - [POST /{reviewUuid}/vote](#post-reviewuuidvote)
- [Review Images](#review-images)
  - [POST /{reviewUuid}/images](#post-reviewuuidimages)
  - [GET /{reviewUuid}/images](#get-reviewuuidimages)
- [Internal Endpoints](#internal-endpoints)
- [Error Responses](#error-responses)
- [Database Schema](#database-schema)

---

## Overview

The Review Service allows buyers to leave reviews and ratings on products they have purchased. Reviews include a 1–5 star rating, title, text body, and optional images. Other users can vote reviews as helpful or not. When a review is created or updated, the service calls the Product Service to update the product's average rating.

**Key Rules:**
- Only buyers who have purchased the product can leave a review
- One review per buyer per product
- Reviews can include multiple images
- Helpful/unhelpful voting by any authenticated user

---

## Reviews

---

### POST / (Create Review)

Create a review for a purchased product.

**Auth Required:** Yes (BUYER only — must have purchased the product)

```http
POST /api/review
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "productUuid": "prod-uuid-1",
  "rating": 5,
  "title": "Excellent laptop!",
  "comment": "Great performance, amazing display, and fast delivery. Battery lasts all day. Highly recommend!",
  "images": [
    "https://example.com/review-img-1.jpg",
    "https://example.com/review-img-2.jpg"
  ]
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| productUuid | string | Yes | Valid product UUID |
| rating | int | Yes | 1–5 |
| title | string | Yes | Max 200 chars |
| comment | string | No | Max 2000 chars |
| images | string[] | No | URLs |

**Response (201 Created):**

```json
{
  "uuid": "review-uuid-1",
  "productUuid": "prod-uuid-1",
  "userUuid": "user-uuid-1",
  "userName": "John Doe",
  "rating": 5,
  "title": "Excellent laptop!",
  "comment": "Great performance, amazing display, and fast delivery...",
  "images": [
    {
      "uuid": "img-uuid-1",
      "imageUrl": "https://example.com/review-img-1.jpg"
    },
    {
      "uuid": "img-uuid-2",
      "imageUrl": "https://example.com/review-img-2.jpg"
    }
  ],
  "helpfulVotes": 0,
  "unhelpfulVotes": 0,
  "verified": true,
  "createdAt": "2026-03-06T12:00:00",
  "updatedAt": "2026-03-06T12:00:00"
}
```

**Side Effects:**
- Product Service is called via Feign to update `averageRating` and `totalReviews`


---

### PUT /{uuid}

Update an existing review. Only the review author can update.

**Auth Required:** Yes (BUYER — own review only)

```http
PUT /api/review/review-uuid-1
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "rating": 4,
  "title": "Good laptop, minor issues",
  "comment": "Great overall but the trackpad could be better."
}
```

All fields are optional — only provided fields are updated.

**Response (200 OK):** Updated `ReviewResponse`

Side effect: Product average rating recalculated.


---

### DELETE /{uuid}

Delete a review. Author or admin can delete.

**Auth Required:** Yes (BUYER — own review, or ADMIN)

```http
DELETE /api/review/review-uuid-1
Authorization: Bearer eyJhbGci...
```

**Response:** 204 No Content

Side effect: Product average rating recalculated.


---

### GET /product/{productUuid}

List all reviews for a product (paginated, sorted by most recent).

**Auth Required:** No (Public)

```http
GET /api/review/product/prod-uuid-1?page=0&size=10&sort=createdAt,desc
```

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| page | int | No | Default: 0 |
| size | int | No | Default: 10 |
| sort | string | No | e.g., `rating,desc` or `createdAt,desc` |

**Response (200 OK):**

```json
{
  "content": [
    {
      "uuid": "review-uuid-1",
      "userUuid": "user-uuid-1",
      "userName": "John Doe",
      "rating": 5,
      "title": "Excellent laptop!",
      "comment": "Great performance...",
      "images": [...],
      "helpfulVotes": 12,
      "unhelpfulVotes": 1,
      "verified": true,
      "createdAt": "2026-03-06T12:00:00"
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "number": 0,
  "size": 10
}
```


---

### GET /my-reviews

List the authenticated user's reviews.

**Auth Required:** Yes (BUYER)

```http
GET /api/review/my-reviews?page=0&size=10
```

**Response (200 OK):** Paginated `ReviewResponse` list


---

## Votes

---

### POST /{reviewUuid}/vote

Vote a review as helpful or unhelpful. One vote per user per review (toggle).

**Auth Required:** Yes (any authenticated user)

```http
POST /api/review/review-uuid-1/vote
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "helpful": true
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| helpful | boolean | Yes | `true` = helpful, `false` = unhelpful |

**Response (200 OK):**

```json
{
  "reviewUuid": "review-uuid-1",
  "helpfulVotes": 13,
  "unhelpfulVotes": 1,
  "userVote": "HELPFUL"
}
```

**Behavior:**
- First vote: Creates vote record
- Same vote again: Removes vote (toggle off)
- Different vote: Changes vote direction


---

## Review Images

---

### POST /{reviewUuid}/images

Add images to an existing review.

**Auth Required:** Yes (BUYER — own review only)

```http
POST /api/review/review-uuid-1/images
Content-Type: application/json
```

```json
{
  "images": [
    "https://example.com/review-img-3.jpg"
  ]
}
```

**Response (201 Created):** Updated image list


---

### GET /{reviewUuid}/images

Get all images for a review.

**Auth Required:** No (Public)

```http
GET /api/review/review-uuid-1/images
```

**Response (200 OK):**

```json
[
  {
    "uuid": "img-uuid-1",
    "imageUrl": "https://example.com/review-img-1.jpg",
    "displayOrder": 1
  },
  {
    "uuid": "img-uuid-2",
    "imageUrl": "https://example.com/review-img-2.jpg",
    "displayOrder": 2
  }
]
```


---

## Internal Endpoints

Secured by `X-Internal-Secret` header. Not routed through API Gateway.

| Method | Endpoint | Called By | Purpose |
|:------:|----------|-----------|---------|
| GET | `/internal/product/{productUuid}/stats` | Product Service | Get review count and average for a product |

---

## Error Responses

```json
{
  "timestamp": "2026-03-06T12:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You must purchase this product before reviewing it",
  "path": "/api/review"
}
```

| Status | Common Causes |
|:------:|---------------|
| 400 | Invalid rating (not 1–5), missing required fields |
| 401 | Missing/invalid JWT |
| 403 | Not a buyer, haven't purchased the product, not review owner |
| 404 | Review/product not found |
| 409 | Already reviewed this product |

---

## Database Schema

### reviews

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| product_uuid | VARCHAR | NOT NULL |
| user_uuid | VARCHAR | NOT NULL |
| user_name | VARCHAR | |
| rating | INT | NOT NULL, 1–5 |
| title | VARCHAR(200) | NOT NULL |
| comment | VARCHAR(2000) | |
| helpful_votes | INT | Default 0 |
| unhelpful_votes | INT | Default 0 |
| verified | BOOLEAN | Default false (true if purchase verified) |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Unique constraint:** `(product_uuid, user_uuid)` — one review per user per product.

### review_images

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| review_id | BIGINT | FK → reviews |
| image_url | VARCHAR | NOT NULL |
| display_order | INT | |

### review_votes

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| review_id | BIGINT | FK → reviews |
| user_uuid | VARCHAR | NOT NULL |
| vote_type | VARCHAR | HELPFUL, UNHELPFUL |
| created_at | TIMESTAMP | |

**Unique constraint:** `(review_id, user_uuid)` — one vote per user per review.

