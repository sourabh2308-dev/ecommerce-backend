# Product Service — API Documentation

**Base URL:** `http://localhost:8080/api/product` and `http://localhost:8080/api/category`  
**Internal Port:** 8080  
**Database:** product_db (PostgreSQL)  
**Cache:** Redis (10-min TTL)  
**Search:** Elasticsearch 8  
**Events:** Kafka (inventory events)

---

## Table of Contents

- [Overview](#overview)
- [Products](#products)
  - [POST / (Create)](#post--create-product)
  - [GET /{uuid}](#get-uuid)
  - [PUT /{uuid}](#put-uuid)
  - [DELETE /{uuid}](#delete-uuid)
  - [GET /search](#get-search)
  - [GET /category/{categoryUuid}](#get-categorycategoryuuid)
  - [PUT /admin/approve/{uuid}](#put-adminapproveuuid)
  - [PUT /admin/block/{uuid}](#put-adminblockuuid)
  - [PUT /admin/unblock/{uuid}](#put-adminunblockuuid)
- [Categories](#categories)
  - [POST /api/category](#post-apicategory)
  - [GET /api/category/{uuid}](#get-apicategoryuuid)
  - [PUT /api/category/{uuid}](#put-apicategoryuuid)
  - [DELETE /api/category/{uuid}](#delete-apicategoryuuid)
  - [GET /api/category/root](#get-apicategoryroot)
  - [GET /api/category/{parentUuid}/children](#get-apicategoryparentuuidchildren)
  - [GET /api/category/hierarchy/all](#get-apicategoryhierarchyall)
  - [POST /api/category/reorder](#post-apicategoryreorder)
- [Product Images](#product-images)
  - [POST /{productUuid}/images](#post-productuuidimages)
  - [GET /{productUuid}/images](#get-productuuidimages)
  - [DELETE /{productUuid}/images/{imageId}](#delete-productuuidimagesimgid)
- [Product Variants](#product-variants)
  - [POST /{productUuid}/variants](#post-productuuidvariants)
  - [GET /{productUuid}/variants](#get-productuuidvariants)
  - [PUT /variants/{variantUuid}](#put-variantsvariantuuid)
  - [DELETE /variants/{variantUuid}](#delete-variantsvariantuuid)
- [Flash Deals](#flash-deals)
  - [POST /deals](#post-deals)
  - [GET /deals/active](#get-dealsactive)
  - [GET /deals/my](#get-dealsmy)
  - [DELETE /deals/{dealUuid}](#delete-dealsdealuuid)
- [Internal Endpoints](#internal-endpoints)
- [Error Responses](#error-responses)
- [Database Schema](#database-schema)

---

## Overview

The Product Service manages the product catalog, including product CRUD, hierarchical categories, variants (size/color/SKU), images, flash deals, and Elasticsearch-powered search. Products follow an approval workflow (DRAFT → ACTIVE) with admin moderation.

**Product Status Lifecycle:**
```
DRAFT → (admin approves) → ACTIVE
ACTIVE → (admin blocks) → BLOCKED → (admin unblocks) → ACTIVE
ACTIVE → (stock hits 0) → OUT_OF_STOCK → (stock restored) → ACTIVE
ACTIVE → (seller/admin deletes) → DELETED (soft delete)
```

---

## Products

---

### POST / (Create Product)

Create a new product. The product starts in DRAFT status and requires admin approval before it becomes visible.

**Auth Required:** Yes (SELLER only)

```http
POST /api/product
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "name": "Laptop Pro X 2026",
  "description": "High-performance laptop with 16GB RAM, 512GB SSD, and M3 chip",
  "price": 79999.00,
  "stock": 50,
  "category": "Electronics",
  "imageUrl": "https://example.com/images/laptop-pro-x.jpg"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| name | string | Yes | Max 255 chars |
| description | string | No | Max 2000 chars |
| price | double | Yes | Positive |
| stock | int | Yes | Min 0 |
| category | string | Yes | Category name or UUID |
| imageUrl | string | No | URL |

**Response (201 Created):**

```json
{
  "uuid": "prod-uuid-1",
  "name": "Laptop Pro X 2026",
  "description": "High-performance laptop...",
  "price": 79999.00,
  "stock": 50,
  "category": "Electronics",
  "categoryRef": {
    "uuid": "cat-uuid-1",
    "name": "Electronics"
  },
  "sellerUuid": "seller-uuid-1",
  "status": "DRAFT",
  "averageRating": 0.0,
  "totalReviews": 0,
  "imageUrl": "https://example.com/images/laptop-pro-x.jpg",
  "createdAt": "2026-03-06T12:00:00"
}
```


---

### GET /{uuid}

Get product details by UUID.

**Auth Required:** No (Public)

```http
GET /api/product/prod-uuid-1
```

**Response (200 OK):** `ProductResponse` (same as above, with current status and ratings)

| Status | Condition |
|:------:|-----------|
| 200 | Product found |
| 404 | Product not found or deleted |


---

### PUT /{uuid}

Update a product. Seller can update their own products. Admin can update any product.

**Auth Required:** Yes (SELLER or ADMIN)

```http
PUT /api/product/prod-uuid-1
Content-Type: application/json
Authorization: Bearer eyJhbGci...
```

```json
{
  "name": "Laptop Pro X 2026 (Updated)",
  "price": 74999.00,
  "stock": 45
}
```

All fields are optional — only provided fields are updated.

**Response (200 OK):** Updated `ProductResponse`


---

### DELETE /{uuid}

Soft-delete a product. Sets `isDeleted = true`.

**Auth Required:** Yes (SELLER or ADMIN)

```http
DELETE /api/product/prod-uuid-1
Authorization: Bearer eyJhbGci...
```

**Response (200 OK):**

```json
"Product deleted successfully"
```


---

### GET /search

Full-text product search powered by Elasticsearch. Results are cached in Redis.

**Auth Required:** No (Public)

```http
GET /api/product/search?q=laptop&category=electronics&minPrice=50000&maxPrice=100000&page=0&size=20
```

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| q | string | No | Search query (full-text) |
| category | string | No | Filter by category |
| minPrice | double | No | Minimum price |
| maxPrice | double | No | Maximum price |
| page | int | No | Page number (default: 0) |
| size | int | No | Page size (default: 20) |

**Response (200 OK):**

```json
{
  "content": [
    {
      "uuid": "prod-uuid-1",
      "name": "Laptop Pro X 2026",
      "price": 79999.00,
      "stock": 50,
      "category": "Electronics",
      "status": "ACTIVE",
      "averageRating": 4.5,
      "totalReviews": 12,
      "imageUrl": "https://example.com/laptop.jpg"
    }
  ],
  "hasNext": false,
  "cursor": null
}
```


---

### GET /category/{categoryUuid}

List products by category (paginated).

**Auth Required:** No (Public)

```http
GET /api/product/category/cat-uuid-1?page=0&size=20
```

**Response (200 OK):** Paginated `ProductResponse` list


---

### PUT /admin/approve/{uuid}

Approve a DRAFT product, making it visible in the catalog.

**Auth Required:** Yes (ADMIN only)

```http
PUT /api/product/admin/approve/prod-uuid-1
Authorization: Bearer eyJhbGci... (admin token)
```

**Response (200 OK):**

```json
"Product approved successfully"
```

Status change: `DRAFT → ACTIVE`. Product is indexed in Elasticsearch.


---

### PUT /admin/block/{uuid}

Block a product (hide from catalog).

**Auth Required:** Yes (ADMIN only)

```http
PUT /api/product/admin/block/prod-uuid-1
```

**Response (200 OK):** `"Product blocked successfully"`


---

### PUT /admin/unblock/{uuid}

Unblock a previously blocked product.

**Auth Required:** Yes (ADMIN only)

```http
PUT /api/product/admin/unblock/prod-uuid-1
```

**Response (200 OK):** `"Product unblocked successfully"`


---

## Categories

**Base Path:** `/api/category`

Categories support an unlimited nesting hierarchy. A category with `parentUuid = null` is a root category.

---

### POST /api/category

Create a new category.

**Auth Required:** Yes (ADMIN only)

```json
{
  "name": "Electronics",
  "description": "Electronic devices and gadgets",
  "imageUrl": "https://example.com/electronics.jpg",
  "parentUuid": null,
  "displayOrder": 1
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| name | string | Yes | Max 100 chars |
| description | string | No | Max 500 chars |
| imageUrl | string | No | Category image |
| parentUuid | string | No | null = root category |
| displayOrder | int | No | Sort position among siblings |

**Response (201 Created):**

```json
{
  "uuid": "cat-uuid-1",
  "name": "Electronics",
  "description": "Electronic devices and gadgets",
  "imageUrl": "https://example.com/electronics.jpg",
  "parentUuid": null,
  "displayOrder": 1,
  "isActive": true,
  "children": [],
  "createdAt": "2026-03-06T12:00:00"
}
```


---

### GET /api/category/{uuid}

Get category details.

**Auth Required:** No (Public)

```http
GET /api/category/cat-uuid-1
```


---

### PUT /api/category/{uuid}

Update a category.

**Auth Required:** Yes (ADMIN only)

```http
PUT /api/category/cat-uuid-1
```

Request body: Same as POST.


---

### DELETE /api/category/{uuid}

Delete a category and its children.

**Auth Required:** Yes (ADMIN only)

```http
DELETE /api/category/cat-uuid-1
```

**Response:** 204 No Content


---

### GET /api/category/root

List all root categories (top-level).

**Auth Required:** No (Public)

```http
GET /api/category/root
```

**Response (200 OK):**

```json
[
  {
    "uuid": "cat-uuid-1",
    "name": "Electronics",
    "displayOrder": 1,
    "children": []
  },
  {
    "uuid": "cat-uuid-2",
    "name": "Clothing",
    "displayOrder": 2,
    "children": []
  }
]
```


---

### GET /api/category/{parentUuid}/children

List child categories of a given parent.

```http
GET /api/category/cat-uuid-1/children
```


---

### GET /api/category/hierarchy/all

Get the full category tree with all nesting levels populated.

```http
GET /api/category/hierarchy/all
```

**Response (200 OK):**

```json
[
  {
    "uuid": "cat-uuid-1",
    "name": "Electronics",
    "children": [
      {
        "uuid": "cat-uuid-3",
        "name": "Laptops",
        "children": []
      },
      {
        "uuid": "cat-uuid-4",
        "name": "Phones",
        "children": []
      }
    ]
  }
]
```


---

### POST /api/category/reorder

Reorder sibling categories under a parent.

**Auth Required:** Yes (ADMIN only)

```json
{
  "parentUuid": "cat-uuid-1",
  "orderedUuids": ["cat-uuid-4", "cat-uuid-3"]
}
```

**Response:** 204 No Content


---

## Product Images

---

### POST /{productUuid}/images

Add an image to a product.

**Auth Required:** Yes (SELLER only)

```http
POST /api/product/prod-uuid-1/images
Content-Type: application/json
```

```json
{
  "imageUrl": "https://example.com/laptop-side.jpg",
  "displayOrder": 2,
  "alt": "Laptop side view"
}
```

| Field | Type | Required |
|-------|------|:--------:|
| imageUrl | string | Yes |
| displayOrder | int | No |
| alt | string | No |

**Response (201 Created):**

```json
{
  "id": 1,
  "productUuid": "prod-uuid-1",
  "imageUrl": "https://example.com/laptop-side.jpg",
  "displayOrder": 2,
  "alt": "Laptop side view"
}
```


---

### GET /{productUuid}/images

List all images for a product.

**Auth Required:** No (Public)

```http
GET /api/product/prod-uuid-1/images
```

**Response (200 OK):** Array of `ImageResponse`


---

### DELETE /{productUuid}/images/{imageId}

Remove an image from a product.

**Auth Required:** Yes (SELLER only)

```http
DELETE /api/product/prod-uuid-1/images/1
```


---

## Product Variants

---

### POST /{productUuid}/variants

Add a variant (size, color, etc.) to a product.

**Auth Required:** Yes (SELLER only)

```http
POST /api/product/prod-uuid-1/variants
Content-Type: application/json
```

```json
{
  "variantName": "Size",
  "variantValue": "XL",
  "priceOverride": 84999.00,
  "stock": 15,
  "sku": "LAPTOP-PRO-X-XL"
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| variantName | string | Yes | e.g., "Size", "Color" |
| variantValue | string | Yes | e.g., "XL", "Red" |
| priceOverride | double | No | null = use base product price |
| stock | int | Yes | Min 0 |
| sku | string | Yes | Unique SKU code |

**Response (201 Created):**

```json
{
  "uuid": "var-uuid-1",
  "variantName": "Size",
  "variantValue": "XL",
  "priceOverride": 84999.00,
  "stock": 15,
  "sku": "LAPTOP-PRO-X-XL",
  "isActive": true
}
```


---

### GET /{productUuid}/variants

List all variants for a product.

**Auth Required:** No (Public)

```http
GET /api/product/prod-uuid-1/variants
```

**Response (200 OK):** Array of `VariantResponse`


---

### PUT /variants/{variantUuid}

Update a variant.

**Auth Required:** Yes (SELLER only)

```http
PUT /api/product/variants/var-uuid-1
```

Request body: Same as POST.


---

### DELETE /variants/{variantUuid}

Delete a variant.

**Auth Required:** Yes (SELLER only)

```http
DELETE /api/product/variants/var-uuid-1
```


---

## Flash Deals

**Base Path:** `/api/product/deals`

Time-limited promotions with percentage discounts.

---

### POST /deals

Create a flash deal for a product.

**Auth Required:** Yes (SELLER only)

```json
{
  "productUuid": "prod-uuid-1",
  "discountPercent": 20.0,
  "startTime": "2026-03-07T00:00:00",
  "endTime": "2026-03-08T23:59:59"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| productUuid | string | Yes | Must be seller's product |
| discountPercent | double | Yes | 1.0–99.0 |
| startTime | datetime | Yes | ISO 8601 |
| endTime | datetime | Yes | Must be after startTime |

**Response (201 Created):**

```json
{
  "uuid": "deal-uuid-1",
  "productUuid": "prod-uuid-1",
  "sellerUuid": "seller-uuid-1",
  "discountPercent": 20.0,
  "startTime": "2026-03-07T00:00:00",
  "endTime": "2026-03-08T23:59:59",
  "isActive": false,
  "createdAt": "2026-03-06T12:00:00"
}
```

`isActive` becomes `true` automatically when `startTime` is reached.


---

### GET /deals/active

List all currently active flash deals.

**Auth Required:** No (Public)

```http
GET /api/product/deals/active
```


---

### GET /deals/my

List the seller's own flash deals.

**Auth Required:** Yes (SELLER only)

```http
GET /api/product/deals/my
```


---

### DELETE /deals/{dealUuid}

Cancel a flash deal.

**Auth Required:** Yes (SELLER only)

```http
DELETE /api/product/deals/deal-uuid-1
```


---

## Internal Endpoints

These are called by other services (not through the gateway). Secured by `X-Internal-Secret`.

| Method | Endpoint | Called By | Purpose |
|:------:|----------|-----------|---------|
| POST | `/internal/{uuid}/reduce-stock?qty=2` | Order Service | Reduce stock when order is placed |
| POST | `/internal/{uuid}/restore-stock?qty=2` | Order Service | Restore stock on order cancellation |
| PUT | `/internal/{uuid}/update-rating?rating=4.5` | Review Service | Update average rating after review |

---

## Error Responses

```json
{
  "timestamp": "2026-03-06T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/product/invalid-uuid"
}
```

| Status | Common Causes |
|:------:|---------------|
| 400 | Validation failure |
| 401 | Missing/invalid JWT |
| 403 | Not SELLER/ADMIN, or not product owner |
| 404 | Product/category not found |
| 409 | Duplicate SKU |

---

## Database Schema

### products

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| name | VARCHAR(255) | NOT NULL |
| description | VARCHAR(2000) | |
| price | DOUBLE | NOT NULL |
| stock | INT | NOT NULL |
| category | VARCHAR | |
| category_id | BIGINT | FK → categories |
| seller_uuid | VARCHAR | NOT NULL |
| status | VARCHAR | DRAFT, ACTIVE, BLOCKED, OUT_OF_STOCK |
| is_deleted | BOOLEAN | |
| average_rating | DOUBLE | Default 0.0 |
| total_reviews | INT | Default 0 |
| image_url | VARCHAR | |
| low_stock_threshold | INT | Default 10 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### categories

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| uuid | VARCHAR | UNIQUE |
| name | VARCHAR(100) | NOT NULL |
| description | VARCHAR(500) | |
| image_url | VARCHAR | |
| parent_category_id | BIGINT | FK → categories (nullable) |
| display_order | INT | |
| is_active | BOOLEAN | |

### product_images

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| product_id | BIGINT (FK → products) |
| image_url | VARCHAR |
| alt_text | VARCHAR |
| display_order | INT |

### product_variants

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| product_id | BIGINT (FK → products) |
| variant_name | VARCHAR |
| variant_value | VARCHAR |
| sku | VARCHAR (UNIQUE) |
| price | DOUBLE (nullable) |
| stock | INT |
| is_active | BOOLEAN |

### flash_deals

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| uuid | VARCHAR (UNIQUE) |
| product_id | BIGINT (FK → products) |
| discount_percent | DOUBLE |
| start_date_time | TIMESTAMP |
| end_date_time | TIMESTAMP |
| is_active | BOOLEAN |

### inventory_history

| Column | Type |
|--------|------|
| id | BIGINT (PK) |
| product_id | BIGINT (FK → products) |
| event_type | VARCHAR (CREATED, PURCHASED, RETURNED, ADJUSTED) |
| quantity | INT |
| reason | VARCHAR |
| related_order_id | VARCHAR |
| created_at | TIMESTAMP |

