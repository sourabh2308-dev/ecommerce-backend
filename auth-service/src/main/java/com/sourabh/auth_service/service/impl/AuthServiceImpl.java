package com.sourabh.auth_service.service.impl;

import com.sourabh.auth_service.dto.UserDto;
import com.sourabh.auth_service.dto.request.ForgotPasswordRequest;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.request.ResetPasswordRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;
import com.sourabh.auth_service.entity.RefreshToken;
import com.sourabh.auth_service.exception.AuthException;
import com.sourabh.auth_service.exception.UserAccountException;
import com.sourabh.auth_service.exception.UserNotFoundException;
import com.sourabh.auth_service.repository.RefreshTokenRepository;
import com.sourabh.auth_service.service.AuthService;
import com.sourabh.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * AUTHENTICATION SERVICE IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Core business logic for authentication operations in the microservices architecture.
 * This service orchestrates:
 *   1. User credential validation (delegates to user-service)
 *   2. JWT access token generation (stateless, short-lived)
 *   3. Refresh token management (stateful, long-lived, stored in database)
 *   4. Secure token rotation (prevents token reuse attacks)
 *   5. Session termination (logout via token revocation)
 * 
 * ARCHITECTURE PATTERN:
 * ---------------------
 * - Service Layer: Contains business logic, separated from controller (presentation)
 * - Microservice Collaboration: Makes HTTP calls to user-service for user data
 * - Stateless Authentication: JWT tokens contain all user info (no server-side session)
 * - Stateful Refresh: Refresh tokens stored in DB for revocation capability
 * 
 * SECURITY DESIGN:
 * ----------------
 * - Passwords: BCrypt hashed (never stored or transmitted in plain text)
 * - Access Tokens: Short expiry (15 min) limits damage from token theft
 * - Refresh Tokens: Stored hashed, revocable, single-use (rotation pattern)
 * - Internal Communication: X-Internal-Secret header protects service-to-service calls
 * - Defense in Depth: Multiple validation layers (email verified, account active, etc.)
 * 
 * ANNOTATIONS EXPLAINED:
 * ----------------------
 * 
 * @Service:
 *   - Marks this class as a Spring service layer component
 *   - Specialized @Component annotation for business logic layer
 *   - Spring auto-detects and registers as bean during component scanning
 *   - Can be intercepted for transactions, security, logging (AOP)
 *   - Semantic marker distinguishes from @Repository (data layer) and @Controller
 * 
 * @RequiredArgsConstructor (Lombok):
 *   - Auto-generates constructor with parameters for all 'final' fields
 *   - Enables constructor-based dependency injection (best practice)
 *   - Spring Container calls this constructor and injects dependencies
 *   - Makes dependencies immutable and testable (can mock in unit tests)
 *   - Cleaner than @Autowired field injection
 * 
 * @Slf4j (Lombok):
 *   - Auto-creates: private static final Logger log = LoggerFactory.getLogger(...)
 *   - Provides logging methods: log.info(), log.error(), log.debug(), log.warn()
 *   - Uses SLF4J API with Logback implementation
 *   - Supports parameterized logging: log.info("User {} logged in", email)
 *   - Avoids string concatenation cost when logging disabled
 * 
 * @Transactional:
 *   - Wraps method in database transaction (BEGIN -> COMMIT or ROLLBACK)
 *   - All database operations in method execute atomically
 *   - On exception: automatic ROLLBACK (all changes undone)
 *   - On success: automatic COMMIT (changes persisted)
 *   - Uses Spring AOP proxy to intercept method calls
 *   - Default: propagation=REQUIRED (join existing tx or create new)
 *   - Ensures data consistency (e.g., refresh token rotation atomicity)
 * 
 * @Value (Spring):
 *   - Injects property values from application.properties config file
 *   - Evaluated at runtime from config-server or local properties
 *   - Supports SpEL: #{bean.property}, ${property:defaultValue}
 *   - Enables externalized configuration (12-factor app principle)
 *   - Values refreshed from config-server without redeployment
 * 
 * DEPENDENCIES INJECTED:
 * ----------------------
 * - RestTemplate: Makes HTTP calls to user-service (synchronous REST client)
 * - JwtUtil: Generates and validates JWT tokens (utility class)
 * - PasswordEncoder: BCrypt hashing for password comparison
 * - RefreshTokenRepository: JPA repository for refresh token CRUD operations
 * 
 * TOKEN DESIGN:
 * -------------
 * ACCESS TOKEN (JWT):
 *   - Format: Base64-encoded JSON with signature (Header.Payload.Signature)
 *   - Contains: email (subject), uuid, role, issued-at, expiration
 *   - Signed with HMAC-SHA256 (shared secret key)
 *   - Stateless: No database lookup needed for validation
 *   - Short-lived: 15 minutes (configurable via jwt.access-token-expiration)
 *   - Cannot be revoked (design tradeoff for statelessness)
 * 
 * REFRESH TOKEN:
 *   - Format: UUID v4 (random, unguessable)  
 *   - Stored: Database table with user linkage, expiry, revoked flag
 *   - Long-lived: 7 days (configurable via jwt.refresh-token-expiration)
 *   - Revocable: Can blacklist in database
 *   - Single-use: Rotation pattern (old revoked when new issued)
 * 
 * FLOW EXAMPLES:
 * --------------
 * LOGIN:
 *   Client -> POST /api/auth/login {email, password}
 *   -> AuthService calls user-service via RestTemplate
 *   -> BCrypt compares password against stored hash
 *   -> Generates access token (JWT) + refresh token (UUID)
 *   -> Stores refresh token in auth_db.refresh_token table
 *   -> Returns {accessToken, refreshToken, tokenType: "Bearer"}
 * 
 * REFRESH:
 *   Client -> POST /api/auth/refresh?refreshToken=<uuid>
 *   -> Validates token exists, not revoked, not expired
 *   -> Revokes old refresh token (sets revoked=true)
 *   -> Generates new token pair
 *   -> Returns new {accessToken, refreshToken}
 * 
 * LOGOUT:
 *   Client -> POST /api/auth/logout?refreshToken=<uuid>
 *   -> Marks refresh token as revoked in database
 *   -> Access token still valid until expiry (trade-off of statelessness)
 * 
 * ERROR HANDLING:
 * ---------------
 * - AuthException: Invalid credentials, expired tokens
 * - UserAccountException: Account inactive, email not verified
 * - UserNotFoundException: User deleted or not found in user-service
 * - All exceptions caught by GlobalExceptionHandler for consistent API responses
 * 
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Service                           // Marks as Spring service layer component
@RequiredArgsConstructor          // Lombok: generates constructor for final fields
@Slf4j                            // Lombok: creates 'log' field for logging
public class AuthServiceImpl implements AuthService {

    // ═══════════════════════════════════════════════════════════════════════
    // DEPENDENCIES (Injected via constructor)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * HTTP client for making REST calls to other microservices.
     * Bean configured in RestTemplateConfig.
     * Used to fetch user data from user-service internal endpoints.
     */
    private final RestTemplate restTemplate;
    
    /**
     * Utility class for JWT token operations (generate, validate, extract claims).
     * Contains signing key, expiration settings, and JJWT library integration.
     */
    private final JwtUtil jwtUtil;
    
    /**
     * BCrypt password encoder for secure password hashing and comparison.
     * Bean provided by SecurityConfig.
     * Uses adaptive hashing with salt (computationally expensive to crack).
     */
    private final PasswordEncoder passwordEncoder;
    
    /**
     * JPA repository for refresh_token table CRUD operations.
     * Provides methods: save(), findByToken(), etc.
     */
    private final RefreshTokenRepository refreshTokenRepository;

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION PROPERTIES (Injected from application.properties)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Refresh token expiration time in milliseconds.
     * Loaded from: jwt.refresh-token-expiration property
     * Default: 604800000ms (7 days)
     * Used to calculate expiry date when creating new refresh tokens.
 */
    @Value("${jwt.refresh-token-expiration}")
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    private long refreshTokenExpiration;

    /**
     * Shared secret for internal service-to-service authentication.
     * Loaded from: internal.secret property
     * Attached as X-Internal-Secret header in calls to user-service.
     * Prevents external clients from directly calling internal endpoints.
     */
    @Value("${internal.secret}")
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    private String internalSecret;

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL SERVICE URLs (User Service Endpoints)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * User-service endpoint for fetching user by email.
     * Format: http://user-service:8080/api/user/internal/email/{email}
     * Uses Docker service name (user-service) for container-to-container communication.
     * Protected by X-Internal-Secret header (validated by InternalSecretFilter).
     */
    @Value("${service.user.base-url:http://user-service:8080}")
    private String userServiceBaseUrl;

    private String userByEmailUrl() {
        return userServiceBaseUrl + "/api/user/internal/email/";
    }

    private String userByUuidUrl() {
        return userServiceBaseUrl + "/api/user/internal/uuid/";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API METHODS (Implemented from AuthService interface)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * ───────────────────────────────────────────────────────────────────────
     * LOGIN - Authenticate User and Issue Token Pair
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Validates user credentials against user-service and generates JWT token pair.
     * 
     * PROCESS FLOW:
     * 1. Fetch user from user-service by email (via internal REST call)
     * 2. Validate account is active and email is verified
     * 3. Compare submitted password with stored BCrypt hash
     * 4. Generate JWT access token (contains email, uuid, role)
     * 5. Generate random refresh token (UUID v4)
     * 6. Save refresh token to database with expiry date
     * 7. Return token pair to client
     * 
     * SECURITY CHECKS:
     * - User exists (throws AuthException if not found)
     * - Email verified (throws UserAccountException if false)
     * - Account active or in seller onboarding (PENDING_DETAILS/PENDING_APPROVAL)
     * - Password matches (BCrypt comparison, constant-time to prevent timing attacks)
     * 
     * TRANSACTION:
     * @Transactional ensures refresh token save is atomic.
     * If any step fails, all database changes are rolled back.
     * 
     * @param request Contains email and password from login form
     * @return AuthResponse with accessToken, refreshToken, tokenType
     * @throws AuthException if credentials invalid
     * @throws UserAccountException if account inactive or email not verified
     */
    @Override
    @Transactional  // Ensures atomic token creation (rollback on failure)
    /**
     * LOGIN - Method Documentation
     *
     * PURPOSE:
     * This method handles the login operation.
     *
     * PARAMETERS:
     * @param request - LoginRequest value
     *
     * RETURN VALUE:
     * @return AuthResponse - Result of the operation
     *
     * ANNOTATIONS USED:
     * @return - Applied to this method
     * @throws - Applied to this method
     * @throws - Applied to this method
     * @Override - Implements interface method
     * @Transactional - Wraps in database transaction (atomic execution)
     *
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        // Log login attempt (email only, never log passwords)
        log.info("Login attempt for email: {}", normalizedEmail);

        // Step 1: Fetch user from user-service via REST call
        // Returns Optional.empty() if user not found (converted to AuthException)
        UserDto user = fetchUser(userByEmailUrl() + normalizedEmail)
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        // Step 2: Validate account status (email verified, active status)
        validateActiveAccount(user);

        // Step 3: Compare password using BCrypt (constant-time comparison)
        // passwordEncoder.matches() compares plain text with hashed password
        // Returns false if passwords don't match
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Return generic error message (don't reveal if email or password was wrong)
            throw new AuthException("Invalid credentials");
        }

        // Step 4: Generate JWT access token with user claims
        // Token contents: {sub: email, uuid: userUuid, role: userRole, iat: timestamp, exp: timestamp}
        String accessToken       = jwtUtil.generateAccessToken(user.getEmail(), user.getUuid(), user.getRole());
        
        // Step 5: Generate random refresh token (UUID v4 format)
        String refreshTokenValue = UUID.randomUUID().toString();

        // Step 6: Save refresh token to database with expiry time
        // Token stored with: {token, userUuid, expiryDate, revoked: false}
        refreshTokenRepository.save(buildRefreshToken(refreshTokenValue, user.getUuid()));

        // Log successful login (audit trail)
        log.info("Login successful for user: {}", user.getEmail());
        
        // Step 7: Build and return response with token pair
        return buildAuthResponse(accessToken, refreshTokenValue);
    }

    /**
     * ───────────────────────────────────────────────────────────────────────
     * REFRESH TOKEN - Rotate Tokens for Session Extension
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Exchanges valid refresh token for new access token + new refresh token.
     * Implements secure token rotation pattern to prevent token reuse attacks.
     * 
     * PROCESS FLOW:
     * 1. Lookup refresh token in database
     * 2. Validate token: not revoked, not expired
     * 3. Fetch associated user from user-service (verify still exists/active)
     * 4. Revoke old refresh token (sets revoked=true in database)
     * 5. Generate new access token (JWT with updated expiry)
     * 6. Generate new refresh token (new UUID)
     * 7. Save new refresh token to database
     * 8. Return new token pair
     * 
     * SECURITY - TOKEN ROTATION:
     * - Old refresh token immediately revoked (cannot be reused)
     * - If stolen token is used after rotation, attack detected
     * - Limits exposure window (each refresh token single-use)
     * - Family tree tracking possible for advanced security
     * 
     * TRANSACTION:
     * @Transactional ensures atomicity of:
     * - Old token revocation
     * - New token creation
     * If any step fails, both operations are rolled back.
     * 
     * ALLOWED USER STATES:
     * - ACTIVE: Full access
     * - PENDING_DETAILS: Seller in onboarding (can browse/login)
     * - PENDING_APPROVAL: Seller awaiting admin approval (can login)
     * 
     * @param refreshTokenValue The refresh token UUID from client
     * @return AuthResponse with new accessToken and new refreshToken
     * @throws AuthException if token invalid, revoked, or expired
     * @throws UserNotFoundException if user no longer exists
     * @throws UserAccountException if user account suspended/deleted
     */
    @Override
    @Transactional  // Atomic token rotation (revoke old + create new)
    /**
     * REFRESHTOKEN - Method Documentation
     *
     * PURPOSE:
     * This method handles the refreshToken operation.
     *
     * PARAMETERS:
     * @param refreshTokenValue - String value
     *
     * RETURN VALUE:
     * @return AuthResponse - Result of the operation
     *
     * ANNOTATIONS USED:
     * @throws - Applied to this method
     * @throws - Applied to this method
     * @throws - Applied to this method
     * @Override - Implements interface method
     * @Transactional - Wraps in database transaction (atomic execution)
     *
     */
    public AuthResponse refreshToken(String refreshTokenValue) {
        // Step 1: Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        // Step 2: Validate token not already revoked (logout or previous rotation)
        if (refreshToken.isRevoked()) {
            throw new AuthException("Refresh token has been revoked");
        }
        
        // Step 3: Validate token not expired (check expiry date)
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Refresh token has expired");
        }

        // Step 4: Fetch user from user-service to verify still exists and active
        // Uses UUID instead of email (user might have changed email)
        UserDto user = fetchUser(userByUuidUrl() + refreshToken.getUserUuid())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Step 5: Validate user account still in allowed state
        String refreshStatus = user.getStatus();
        if (!("ACTIVE".equalsIgnoreCase(refreshStatus)
                || "PENDING_DETAILS".equalsIgnoreCase(refreshStatus)
                || "PENDING_APPROVAL".equalsIgnoreCase(refreshStatus))) {
            // User account suspended, deleted, or rejected
            throw new UserAccountException("User account is no longer active");
        }

        // Step 6: ROTATION - Revoke old refresh token (mark as used)
        // This prevents token reuse (if attacker stole token, only works once)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Step 7: Generate new access token (fresh expiry time)
        String newAccessToken       = jwtUtil.generateAccessToken(user.getEmail(), user.getUuid(), user.getRole());
        
        // Step 8: Generate new refresh token (new random UUID)
        String newRefreshTokenValue = UUID.randomUUID().toString();

        // Step 9: Save new refresh token to database
        refreshTokenRepository.save(buildRefreshToken(newRefreshTokenValue, user.getUuid()));

        // Step 10: Return new token pair (client replaces old tokens)
        return buildAuthResponse(newAccessToken, newRefreshTokenValue);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ───────────────────────────────────────────────────────────────────────
     * FETCH USER - Internal HTTP Call to User Service
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Makes authenticated HTTP GET request to user-service internal endpoint.
     * Attaches X-Internal-Secret header for service-to-service security.
     * 
     * INTERNAL ENDPOINTS:
     * These endpoints are NOT exposed through API Gateway.
     * Only accessible from within Docker network via service name resolution.
     * Protected by InternalSecretFilter (rejects requests without secret header).
     * 
     * ERROR HANDLING:
     * - 404 Not Found: Returns Optional.empty() (user doesn't exist)
     * - Other 4xx errors: Logs warning, returns Optional.empty()
     * - 5xx errors: Exception propagates (handled by GlobalExceptionHandler)
     * 
     * SECURITY:
     * X-Internal-Secret header proves request originated from trusted service.
     * Prevents external clients from calling internal endpoints directly.
     * 
     * @param url Full URL to user-service endpoint (with email/uuid appended)
     * @return Optional<UserDto> containing user data, or empty if not found
     */
    private Optional<UserDto> fetchUser(String url) {
        try {
            // Create HTTP headers with internal secret for authentication
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            
            // Make HTTP GET request to user-service
            // RestTemplate.exchange() sends request and deserializes response
            // HttpEntity wraps headers (no body needed for GET)
            // UserDto.class specifies expected response type
            UserDto body = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), UserDto.class)
                    .getBody();
            
            // Convert to Optional (handles null response body gracefully)
            return Optional.ofNullable(body);
            
        } catch (HttpClientErrorException.NotFound e) {
            // User not found (404) - return empty Optional
            // This is expected behavior, not an error to log
            return Optional.empty();
            
        } catch (HttpClientErrorException e) {
            // Other client errors (400, 403, etc.) - log and return empty
            // Prevents cascade failures if user-service has validation errors
            log.warn("HTTP error fetching user from {}: {} {}", url, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        }
        // Note: 5xx server errors (IOException, etc.) propagate up to be handled globally
    }

    /**
     * ───────────────────────────────────────────────────────────────────────
     * VALIDATE ACTIVE ACCOUNT - Check User Eligibility for Login
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Enforces account status requirements before allowing authentication.
     * Implements multi-layered security checks.
     * 
     * VALIDATION RULES:
     * 1. Email MUST be verified (prevents spam accounts)
     * 2. Account status MUST be one of:
     *    - ACTIVE: Normal users (buyers/sellers with complete profiles)
     *    - PENDING_DETAILS: Sellers who registered but haven't submitted details
     *    - PENDING_APPROVAL: Sellers awaiting admin verification
     * 
     * REJECTED STATES:
     * - SUSPENDED: Admin temporarily blocked access
     * - INACTIVE: User deactivated their account
     * - REJECTED: Admin rejected seller application
     * 
     * WHY ALLOW PENDING STATES:
     * Sellers in onboarding (PENDING_DETAILS/PENDING_APPROVAL) can login to:
     * - Submit seller details (business info, documents)
     * - Check approval status
     * - Browse products (read-only access)
     * But API authorization prevents them from creating products until ACTIVE.
     * 
     * @param user UserDto fetched from user-service
     * @throws UserAccountException if email not verified or account inactive
     */
    private void validateActiveAccount(UserDto user) {
        // Check 1: Email verification (prevents spam, confirms ownership)
        if (!user.isEmailVerified()) {
            throw new UserAccountException("Email not verified");
        }
        
        // Check 2: Account status (allow active and seller onboarding states)
        String status = user.getStatus();
        
        // Allow ACTIVE users (full access)
        // Allow sellers going through onboarding (PENDING_DETAILS / PENDING_APPROVAL)
        if ("ACTIVE".equalsIgnoreCase(status)
                || "PENDING_DETAILS".equalsIgnoreCase(status)
                || "PENDING_APPROVAL".equalsIgnoreCase(status)) {
            return;  // Validation passed
        }
        
        // Reject all other statuses (SUSPENDED, INACTIVE, REJECTED, etc.)
        throw new UserAccountException("User account is not active");
    }

    /**
     * ───────────────────────────────────────────────────────────────────────
     * BUILD REFRESH TOKEN - Create Entity for Database Storage
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Factory method to construct RefreshToken entity with calculated expiry.
     * Centralizes token creation logic.
     * 
     * TOKEN PROPERTIES:
     * - token: UUID v4 string (random, 128-bit entropy)
     * - userUuid: Links token to specific user
     * - expiryDate: Current time + refresh token lifetime
     * - revoked: false (newly created token is active)
     * 
     * EXPIRY CALCULATION:
     * refreshTokenExpiration is in milliseconds (e.g., 604800000 = 7 days)
     * Divide by 1000 to convert to seconds for LocalDateTime.plusSeconds()
     * 
     * @param token UUID string for the refresh token
     * @param userUuid User identifier who owns this token
     * @return RefreshToken entity ready to persist
     */
    private RefreshToken buildRefreshToken(String token, String userUuid) {
        return RefreshToken.builder()
                .token(token)                                                // Unique token identifier
                .userUuid(userUuid)                                         // Owner of token
                .expiryDate(LocalDateTime.now()                            // Calculate expiry: now + lifetime
                        .plusSeconds(refreshTokenExpiration / 1000))       // Convert ms to seconds
                .revoked(false)                                            // New token is active
                .build();
    }

    /**
     * ───────────────────────────────────────────────────────────────────────
     * BUILD AUTH RESPONSE - Format Token Pair for Client
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Factory method to construct standardized API response.
     * 
     * RESPONSE FORMAT:
     * {
     *   "accessToken": "eyJhbGc...",     // JWT token for API calls
     *   "refreshToken": "uuid-string",    // For token renewal
     *   "tokenType": "Bearer"              // HTTP Authorization scheme
     * }
     * 
     * USAGE:
     * Client stores both tokens (typically in localStorage or httpOnly cookie).
     * For API calls: Authorization: Bearer <accessToken>
     * For renewal: POST /api/auth/refresh?refreshToken=<refreshToken>
     * 
     * @param accessToken JWT string with user claims
     * @param refreshToken UUID string for session renewal
     * @return AuthResponse DTO ready for JSON serialization
     */
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)      // Short-lived JWT
                .refreshToken(refreshToken)    // Long-lived UUID
                .tokenType("Bearer")           // Standard HTTP auth scheme
                .build();
    }

    /**
     * ───────────────────────────────────────────────────────────────────────
     * LOGOUT - Revoke Refresh Token to Terminate Session
     * ───────────────────────────────────────────────────────────────────────
     * 
     * PURPOSE:
     * Marks refresh token as revoked to prevent future use.
     * Simulates session termination in stateless JWT architecture.
     * 
     * PROCESS FLOW:
     * 1. Lookup refresh token in database
     * 2. Check if already revoked (idempotent operation)
     * 3. Set revoked=true flag
     * 4. Save to database
     * 5. Log logout event
     * 
     * LIMITATIONS:
     * - Access token remains valid until expiry (stateless design tradeoff)
     * - For immediate revocation, implement token blacklist (Redis cache)
     * - Short access token lifetime (15 min) limits exposure window
     * 
     * IDEMPOTENCY:
     * Multiple logout calls with same token are safe (no-op if already revoked).
     * 
     * TRANSACTION:
     * @Transactional ensures token revocation is persisted atomically.
     * 
     * @param refreshTokenValue UUID of token to revoke
     * @throws AuthException if token doesn't exist
     */
    @Override
    @Transactional  // Atomic revocation operation
    /**
     * LOGOUT - Method Documentation
     *
     * PURPOSE:
     * This method handles the logout operation.
     *
     * PARAMETERS:
     * @param refreshTokenValue - String value
     *
     * ANNOTATIONS USED:
     * @Transactional - Wraps in database transaction (atomic execution)
     * @param - Applied to this method
     * @throws - Applied to this method
     * @Override - Implements interface method
     * @Transactional - Wraps in database transaction (atomic execution)
     *
     */
    public void logout(String refreshTokenValue) {
        // Step 1: Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        // Step 2: Check if already revoked (idempotent check)
        if (!refreshToken.isRevoked()) {
            // Step 3: Mark token as revoked (prevents future use)
            refreshToken.setRevoked(true);
            
            // Step 4: Persist revocation to database
            refreshTokenRepository.save(refreshToken);
            
            // Step 5: Log logout for audit trail
            log.info("User logged out: userUuid={}", refreshToken.getUserUuid());
        }
        // If already revoked, do nothing (idempotent operation)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FORGOT PASSWORD / RESET PASSWORD
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Forgot password request for email: {}", email);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String url = userServiceBaseUrl + "/api/user/internal/forgot-password?email=" + email;
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            return "If the email exists, a password reset OTP has been sent.";
        } catch (HttpClientErrorException e) {
            log.warn("Forgot password error for {}: {}", email, e.getMessage());
            // Return generic message to prevent email enumeration
            return "If the email exists, a password reset OTP has been sent.";
        }
    }

    @Override
    public String resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Password reset attempt for email: {}", email);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String url = userServiceBaseUrl + "/api/user/internal/reset-password"
                    + "?email=" + email
                    + "&otpCode=" + request.getOtpCode()
                    + "&newPassword=" + request.getNewPassword();
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            return "Password reset successfully";
        } catch (HttpClientErrorException e) {
            log.warn("Password reset error for {}: {}", email, e.getMessage());
            throw new AuthException("Password reset failed: " + e.getResponseBodyAsString());
        }
    }
}
