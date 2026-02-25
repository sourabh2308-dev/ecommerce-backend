package com.sourabh.api_gateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
/**
 * SPRING SECURITY CONFIGURATION - Authentication & Authorization Setup
 * 
 * PURPOSE:
 * Configures Spring Security framework for this microservice.
 * Defines which endpoints require authentication, how tokens are validated,
 * and what roles can access specific resources.
 * 
 * KEY CONCEPTS:
 * 
 * 1. AUTHENTICATION (Who are you?)
 *    - JWT tokens validated by JwtAuthenticationFilter
 *    - User claims extracted and stored in SecurityContext
 * 
 * 2. AUTHORIZATION (What can you do?)
 *    - @PreAuthorize("hasRole('BUYER')") on controller methods
 *    - Checks if authenticated user has required role
 * 
 * CONFIGURATION COMPONENTS:
 * 
 * @Bean SecurityFilterChain:
 *   - Defines URL patterns and access rules
 *   - Example: .requestMatchers("/api/order/**").authenticated()
 *   - Registers custom filters (JWT validation, etc.)
 * 
 * @Bean PasswordEncoder:
 *   - BCrypt for hashing passwords (user-service only)
 *   - Not used in services that don't store passwords
 * 
 * CORS Configuration:
 *   - Allows cross-origin requests from frontend
 *   - Configures allowed origins, methods, headers
 * 
 * STATELESS SESSION:
 *   - sessionCreationPolicy(STATELESS)
 *   - No server-side sessions (JWT is self-contained)
 * 
 * ENDPOINT ACCESS RULES:
 * Common patterns across services:
 * 
 * PUBLIC (No authentication):
 *   - POST /api/user/register
 *   - POST /api/auth/login
 *   - GET /api/product (listing products)
 * 
 * AUTHENTICATED (Any logged-in user):
 *   - GET /api/user/profile
 *   - POST /api/order (role checked in controller)
 * 
 * ROLE-BASED (Specific roles):
 *   - POST /api/product → @PreAuthorize("hasRole('SELLER')")
 *   - GET /api/order/all → @PreAuthorize("hasRole('ADMIN')")
 * 
 * INTERNAL (Service-to-service):
 *   - POST /api/product/internal/** → Validated by InternalSecretFilter
 *   - No JWT required, uses shared secret header
 * 
 * FILTER ORDER:
 * 1. CorsFilter (handle preflight OPTIONS)
 * 2. JwtAuthenticationFilter (extract user from token)
 * 3. Spring Security filters (authorization checks)
 * 4. Controller method execution
 */
public class JwtAuthenticationManager implements
        org.springframework.security.authentication.ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication)
            throws AuthenticationException {

        String token = authentication.getCredentials().toString();

        try {
            Claims claims = jwtUtil.validateToken(token);

            String role = claims.get("role", String.class);
            String uuid = claims.get("uuid", String.class);
            String email = claims.getSubject();

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );
            // Store uuid so ClaimsForwardingFilter can read it without re-parsing
            auth.setDetails(uuid);

            return Mono.just(auth);

        } catch (Exception e) {
            // Return error so Spring Security properly sends 401 Unauthorized
            return Mono.error(new BadCredentialsException("Invalid or expired JWT token"));
        }
    }
}
