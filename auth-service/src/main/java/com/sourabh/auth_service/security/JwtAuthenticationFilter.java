package com.sourabh.auth_service.security;

import com.sourabh.auth_service.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
// HTTP Filter - Intercepts requests for cross-cutting concerns
/**
 * HTTP FILTER - Request/Response Interceptor
 * 
 * Intercepts every HTTP request/response for cross-cutting concerns:
 *   - JWT validation
 *   - Header injection
 *   - Request/response logging
 *   - Rate limiting
 * 
 * Executes before controller and after response generation.
 */
/**
 * HTTP REQUEST/RESPONSE FILTER - Interceptor for Cross-Cutting Concerns
 * 
 * PURPOSE:
 * Intercepts every HTTP request and response passing through this service.
 * Implements cross-cutting concerns like authentication, logging, header
 * injection, request validation before reaching controller methods.
 * 
 * FILTER CHAIN:
 * Request → Filter1 → Filter2 → ... → Controller → ... → Filter2 → Filter1 → Response
 * 
 * EXECUTION ORDER:
 * Controlled by @Order annotation (lower number = higher priority)
 * Common order:
 *   1. @Order(1): CORS filter
 *   2. @Order(2): Authentication filter (JWT validation)
 *   3. @Order(3): Authorization filter (role checks)
 *   4. @Order(4): Logging filter
 *   5. @Order(5): Rate limiting filter
 * 
 * FILTER TYPES:
 * 
 * 1. JwtAuthenticationFilter:
 *    - Validates JWT token from Authorization header
 *    - Extracts user claims (uuid, role, email)
 *    - Sets Spring Security context for @PreAuthorize to work
 * 
 * 2. HeaderInjectionFilter:
 *    - Injects custom headers (X-User-UUID, X-User-Role)
 *    - Used by downstream services/controllers
 * 
 * 3. InternalSecretFilter:
 *    - Validates internal service-to-service calls
 *    - Checks X-Internal-Secret header matches configured secret
 * 
 * 4. LoggingFilter:
 *    - Logs request method, path, headers, body
 *    - Logs response status, body, duration
 * 
 * IMPLEMENTATION PATTERN:
 * class MyFilter implements Filter {
 *   @Override
 *   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 *     // Pre-processing (before controller)
 *     HttpServletRequest request = (HttpServletRequest) req;
 *     
 *     // Pass to next filter/controller
 *     chain.doFilter(request, response);
 *     
 *     // Post-processing (after controller)
 *     HttpServletResponse response = (HttpServletResponse) res;
 *   }
 * }
 */
/**
 * HTTP REQUEST/RESPONSE FILTER - Interceptor for Cross-Cutting Concerns
 * 
 * PURPOSE:
 * Intercepts every HTTP request and response passing through this service.
 * Implements cross-cutting concerns like authentication, logging, header
 * injection, request validation before reaching controller methods.
 * 
 * FILTER CHAIN:
 * Request → Filter1 → Filter2 → ... → Controller → ... → Filter2 → Filter1 → Response
 * 
 * EXECUTION ORDER:
 * Controlled by @Order annotation (lower number = higher priority)
 * Common order:
 *   1. @Order(1): CORS filter
 *   2. @Order(2): Authentication filter (JWT validation)
 *   3. @Order(3): Authorization filter (role checks)
 *   4. @Order(4): Logging filter
 *   5. @Order(5): Rate limiting filter
 * 
 * FILTER TYPES:
 * 
 * 1. JwtAuthenticationFilter:
 *    - Validates JWT token from Authorization header
 *    - Extracts user claims (uuid, role, email)
 *    - Sets Spring Security context for @PreAuthorize to work
 * 
 * 2. HeaderInjectionFilter:
 *    - Injects custom headers (X-User-UUID, X-User-Role)
 *    - Used by downstream services/controllers
 * 
 * 3. InternalSecretFilter:
 *    - Validates internal service-to-service calls
 *    - Checks X-Internal-Secret header matches configured secret
 * 
 * 4. LoggingFilter:
 *    - Logs request method, path, headers, body
 *    - Logs response status, body, duration
 * 
 * IMPLEMENTATION PATTERN:
 * class MyFilter implements Filter {
 *   @Override
 *   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 *     // Pre-processing (before controller)
 *     HttpServletRequest request = (HttpServletRequest) req;
 *     
 *     // Pass to next filter/controller
 *     chain.doFilter(request, response);
 *     
 *     // Post-processing (after controller)
 *     HttpServletResponse response = (HttpServletResponse) res;
 *   }
 * }
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            if (jwtUtil.validateToken(token)) {

                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + role)
                                )
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
