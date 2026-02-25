package com.sourabh.auth_service.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * JWT UTILITY - Token Generation and Validation
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Centralized utility class for all JWT (JSON Web Token) operations.
 * Handles token creation, parsing, validation, and claims extraction.
 * 
 * JWT STRUCTURE:
 * --------------
 * A JWT consists of three Base64-URL encoded parts separated by dots:
 *   Header.Payload.Signature
 * 
 * 1. HEADER (Algorithm & Token Type):
 *    {"alg": "HS256", "typ": "JWT"}
 *    - alg: Signature algorithm (HMAC-SHA256)
 *    - typ: Token type (JWT)
 * 
 * 2. PAYLOAD (Claims - user data):
 *    {
 *      "sub": "user@example.com",  // Subject (email)
 *      "uuid": "user-uuid",         // Custom claim
 *      "role": "BUYER",              // Custom claim
 *      "iat": 1640000000,            // Issued at timestamp
 *      "exp": 1640001000             // Expiration timestamp
 *    }
 * 
 * 3. SIGNATURE (Cryptographic verification):
 *    HMACSHA256(
 *      base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *      secret
 *    )
 * 
 * SECURITY:
 * ---------
 * - Secret Key: 256-bit random string (NEVER commit to version control)
 * - Algorithm: HS256 (HMAC with SHA-256) - symmetric key cryptography
 * - Tamper-Proof: Any modification invalids signature (detected on validation)
 * - Stateless: No database lookup needed (all data in token)
 * - Expiry: Short-lived tokens (15 min) limit damage from theft
 * 
 * WORKFLOW:
 * ---------
 * 1. LOGIN: Generate token with user claims
 * 2. CLIENT: Store token (localStorage/cookie)
 * 3. API CALL: Send token in Authorization: Bearer header
 * 4. GATEWAY: Validate signature and expiry
 * 5. SERVICE: Extract claims (uuid, role) from validated token
 * 
 * ANNOTATIONS:
 * ------------
 * @Component:
 *   - Marks class as Spring-managed bean (utility/helper component)
 *   - Auto-detected during component scanning
 *   - Singleton by default (one instance per application context)
 *   - Injectable into other beans
 * 
 * DEPENDENCIES:
 * -------------
 * - JJWT Library (io.jsonwebtoken.*): Industry-standard JWT implementation
 * - Provides: Jwts builder/parser, Keys generator, SignatureAlgorithm
 * 
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Component  // Spring-managed utility bean
public class JwtUtil {

    /**
     * Cryptographic signing key for HMAC-SHA256 signature.
     * Generated from secret string in application.properties.
     * Used for both signing (token creation) and verifying (token validation).
     */
    private final Key signingKey;
    
    /**
     * Access token lifetime in milliseconds.
     * Injected from jwt.access-token-expiration property.
     * Default: 900000ms (15 minutes)
     * Short expiry limits security risk from token theft.
     */
    private final long accessTokenExpiration;

    /**
     * Constructor for dependency injection of configuration values.
     * 
     * INITIALIZATION:
     * - Converts secret string to cryptographic Key object
     * - Validates secret length (must be ≥ 256 bits for HS256)
     * - Stores expiration time for token generation
     * 
     * @param secret JWT signing secret from application.properties (min 32 chars)
     * @param accessTokenExpiration Token lifetime in milliseconds
     * 
     * @Value Annotation:
     *   - Injects property values from config-server/application.properties
     *   - Supports SpEL expressions: ${property:defaultValue}
     *   - Values loaded at bean creation time
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {

        // Generate HMAC-SHA256 key from secret bytes
        // Keys.hmacShaKeyFor() validates minimum length (256 bits)
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TOKEN GENERATION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generate JWT Access Token with User Claims
     * 
     * PURPOSE:
     * Creates signed JWT containing user identity and authorization data.
     * Token used by API Gateway and services for request authentication.
     * 
     * TOKEN CONTENTS:
     * - sub: Email (standard JWT claim for subject/principal)
     * - uuid: User unique identifier (custom claim)
     * - role: User role (BUYER/SELLER/ADMIN) for authorization
     * - iat: Issued at timestamp (prevents token replay)
     * - exp: Expiration timestamp (token auto-invalidates)
     * 
     * STRUCTURE EXAMPLE:
     * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwidXVpZCI6ImFiYzEyMyIsInJvbGUiOiJCVVlFUiIsImlhdCI6MTY0MDAwMDAwMCwiZXhwIjoxNjQwMDAxMDAwfQ.signature
     * 
     * VALIDATION:
     * - Signature verified using same signingKey
     * - Expiry checked against current time
     * - Claims extracted for authorization decisions
     * 
     * @param email User's email address (becomes JWT subject)
     * @param userUuid User's UUID (primary identifier)
     * @param role User's role (BUYER/SELLER/ADMIN)
     * @return Base64-encoded JWT string ready for HTTP Authorization header
     */
    public String generateAccessToken(
            String email,
            String userUuid,
            String role) {

        return Jwts.builder()
                // Standard claim: subject (usually username or email)
                .setSubject(email)
                
                // Custom claims: user identifiers and authorization data
                // Map.of() creates immutable map with key-value pairs
                .addClaims(Map.of(
                        "uuid", userUuid,    // Primary key for database lookups
                        "role", role         // Authorization role for access control
                ))
                
                // iat (issued at): timestamp when token was created
                // Used to detect old tokens, prevent replay attacks
                .setIssuedAt(new Date())
                
                // exp (expiration): timestamp when token becomes invalid
                // Current time + configured lifetime (e.g., 15 minutes)
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                
                // Sign with HMAC-SHA256 using secret key
                // Signature proves token authenticity (not tampered)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                
                // compact(): Serialize to string (Header.Payload.Signature)
                .compact();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLAIMS EXTRACTION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Extract Email from JWT Token
     * 
     * Retrieves the 'sub' (subject) claim containing user's email.
     * Used by API Gateway to identify the authenticated user.
     * 
     * @param token JWT string from Authorization header
     * @return User's email address
     * @throws JwtException if token invalid or expired
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract User UUID from JWT Token
     * 
     * Retrieves the custom 'uuid' claim containing user's database ID.
     * Used by services for user-specific queries and authorization.
     * 
     * @param token JWT string from Authorization header
     * @return User's UUID
     * @throws JwtException if token invalid or expired
     */
    public String extractUserUuid(String token) {
        // get(key, type): Retrieves custom claim with type-safe casting
        return extractAllClaims(token).get("uuid", String.class);
    }

    /**
     * Extract Role from JWT Token
     * 
     * Retrieves the custom 'role' claim for authorization decisions.
     * Used to enforce role-based access control (RBAC).
     * 
     * ROLES:
     * - BUYER: Can place orders, write reviews
     * - SELLER: Can create products, view sales dashboard
     * - ADMIN: Can verify sellers, moderate content
     * 
     * @param token JWT string from Authorization header
     * @return User's role (BUYER/SELLER/ADMIN)
     * @throws JwtException if token invalid or expired
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TOKEN VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Validate JWT Token Signature and Expiry
     * 
     * PURPOSE:
     * Verifies token authenticity and freshness without throwing exceptions.
     * Used by API Gateway for quick accept/reject decisions.
     * 
     * VALIDATION CHECKS:
     * 1. Signature: Verifies HMAC-SHA256 signature matches
     * 2. Expiry: Checks exp claim against current time
     * 3. Format: Ensures token structure is valid (Header.Payload.Signature)
     * 4. Claims: Parses claims without errors
     * 
     * FAILURE CASES:
     * - Invalid signature (token tampered or wrong secret)
     * - Expired token (exp < now)
     * - Malformed token (missing parts, invalid Base64)
     * - Null/empty token string
     * 
     * @param token JWT string to validate
     * @return true if token valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // Attempt to parse and verify token
            // extractAllClaims() throws exception if invalid
            extractAllClaims(token);
            
            // If parsing succeeded, token is valid
            return true;
            
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException: Signature failed, expired, malformed
            // IllegalArgumentException: Null/invalid input
            
            // Return false instead of throwing (non-exceptional path)
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Parse and Extract All Claims from JWT Token
     * 
     * PURPOSE:
     * Core parsing logic used by all extraction and validation methods.
     * Verifies signature and returns claims object.
     * 
     * PROCESS:
     * 1. Create parser builder with signing key
     * 2. Build parser (configured for HS256 verification)
     * 3. Parse token string (decode Base64, verify signature)
     * 4. Extract claims body (payload portion)
     * 
     * JJWT API:
     * - parserBuilder(): Creates configurable parser
     * - setSigningKey(): Configures key for signature verification
     * - build(): Constructs immutable parser instance
     * - parseClaimsJws(): Parses signed JWT (JWS = JWT with signature)
     * - getBody(): Extracts claims from payload
     * 
     * EXCEPTIONS:
     * - ExpiredJwtException: Token exp claim < current time
     * - SignatureException: Signature verification failed
     * - MalformedJwtException: Invalid token structure
     * - UnsupportedJwtException: Algorithm not supported
     * 
     * @param token JWT string from client
     * @return Claims object containing all token claims (sub, uuid, role, iat, exp)
     * @throws JwtException if token invalid, expired, or tampered
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                // Configure parser with signing key for verification
                .setSigningKey(signingKey)
                
                // Build immutable parser instance
                .build()
                
                // Parse JWT with signature (JWS = Json Web Signature)
                // Automatically verifies signature and expiry
                .parseClaimsJws(token)
                
                // Extract claims from payload (body) section
                .getBody();
    }
}
