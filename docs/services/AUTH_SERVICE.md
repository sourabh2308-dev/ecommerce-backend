# Auth Service — API Documentation

**Base URL:** `http://localhost:8080/api/auth`  
**Internal Port:** 8080  
**Database:** auth_db (PostgreSQL)  
**Dependencies:** User Service (via Feign)

---

## Table of Contents

- [Overview](#overview)
- [Configuration](#configuration)
- [Endpoints](#endpoints)
  - [POST /login](#post-login)
  - [POST /refresh](#post-refresh)
  - [POST /logout](#post-logout)
  - [POST /forgot-password](#post-forgot-password)
  - [POST /reset-password](#post-reset-password)
- [Error Responses](#error-responses)
- [Database Schema](#database-schema)

---

## Overview

The Auth Service handles JWT-based authentication. It does **not** store user credentials — it calls the User Service internally to validate email/password and issue tokens.

**Token Strategy:**
- **Access Token:** JWT (HS256), 15-minute expiry, contains `sub` (email), `uuid`, `role`
- **Refresh Token:** UUID stored in database, 7-day expiry, single-use (rotation)

---

## Configuration

| Property | Value |
|----------|-------|
| `spring.application.name` | auth-service |
| `spring.datasource.url` | jdbc:postgresql://postgres:5432/auth_db |
| `jwt.secret` | `${JWT_SECRET:mysupersecuresecretkeythatislongenough123}` |
| `jwt.access-token-expiration` | 900000 (15 minutes) |
| `jwt.refresh-token-expiration` | 604800000 (7 days) |
| `internal.secret` | `${INTERNAL_SECRET:veryStrongInternalSecret123}` |

---

## Endpoints

---

### POST /login

Authenticate a user with email and password. Returns JWT access token and refresh token.

**Auth Required:** No (Public)

**Request:**

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| email | string | Yes | Valid email format |
| password | string | Yes | Not blank |

**Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXVpZCI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMCIsInJvbGUiOiJCVVlFUiIsImlhdCI6MTcwOTcyMTYwMCwiZXhwIjoxNzA5NzIyNTAwfQ.signature",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001",
  "tokenType": "Bearer"
}
```

| Field | Type | Description |
|-------|------|-------------|
| accessToken | string | JWT access token (15 min expiry) |
| refreshToken | string | UUID refresh token (7 day expiry) |
| tokenType | string | Always "Bearer" |

**Error Responses:**

| Status | Condition |
|:------:|-----------|
| 401 | Invalid email or password |
| 403 | Account not active (blocked, pending verification, etc.) |

<!-- 📸 Postman Screenshot: ![Login](../screenshots/auth_login.png) -->

---

### POST /refresh

Exchange a valid refresh token for a new access token and a new refresh token. The old refresh token is **revoked** (one-time use — token rotation).

**Auth Required:** No (Public)

**Request:**

```http
POST /api/auth/refresh?refreshToken=550e8400-e29b-41d4-a716-446655440001
```

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|:--------:|-------------|
| refreshToken | Query | string (UUID) | Yes | Current refresh token |

**Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...newtoken",
  "refreshToken": "660f9500-f30c-52e5-b827-557766550002",
  "tokenType": "Bearer"
}
```

**Error Responses:**

| Status | Condition |
|:------:|-----------|
| 401 | Refresh token not found, expired, or already revoked |

<!-- 📸 Postman Screenshot: ![Refresh](../screenshots/auth_refresh.png) -->

---

### POST /logout

Revoke a refresh token, effectively logging the user out (the access token remains valid until it naturally expires).

**Auth Required:** No (Public)

**Request:**

```http
POST /api/auth/logout?refreshToken=550e8400-e29b-41d4-a716-446655440001
```

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|:--------:|-------------|
| refreshToken | Query | string (UUID) | Yes | Refresh token to revoke |

**Response (204 No Content):**

No response body.

<!-- 📸 Postman Screenshot: ![Logout](../screenshots/auth_logout.png) -->

---

### POST /forgot-password

Initiate password recovery. Sends a 6-digit OTP to the user's email address.

**Auth Required:** No (Public)

**Request:**

```http
POST /api/auth/forgot-password
Content-Type: application/json
```

```json
{
  "email": "john@example.com"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| email | string | Yes | Valid email, not blank |

**Response (200 OK):**

```json
"OTP sent to your email address"
```

**Error Responses:**

| Status | Condition |
|:------:|-----------|
| 404 | Email not registered |

<!-- 📸 Postman Screenshot: ![Forgot Password](../screenshots/auth_forgot_password.png) -->

---

### POST /reset-password

Complete password reset using the OTP received via email.

**Auth Required:** No (Public)

**Request:**

```http
POST /api/auth/reset-password
Content-Type: application/json
```

```json
{
  "email": "john@example.com",
  "otpCode": "482910",
  "newPassword": "newSecurePassword123"
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| email | string | Yes | Valid email, not blank |
| otpCode | string | Yes | Not blank |
| newPassword | string | Yes | Min 6 characters |

**Response (200 OK):**

```json
"Password reset successful"
```

**Error Responses:**

| Status | Condition |
|:------:|-----------|
| 400 | Invalid or expired OTP |
| 404 | Email not registered |

<!-- 📸 Postman Screenshot: ![Reset Password](../screenshots/auth_reset_password.png) -->

---

## Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2026-03-06T12:00:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/auth/login"
}
```

---

## Database Schema

### refresh_tokens

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-generated |
| token | VARCHAR(255) | UNIQUE, NOT NULL (UUID) |
| user_uuid | VARCHAR(255) | NOT NULL |
| expiry_date | TIMESTAMP | NOT NULL |
| revoked | BOOLEAN | DEFAULT false |
| created_at | TIMESTAMP | Auto-set |
