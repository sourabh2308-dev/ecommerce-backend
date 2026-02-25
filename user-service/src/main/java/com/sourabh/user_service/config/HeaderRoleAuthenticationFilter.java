package com.sourabh.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Reads the {@code X-User-Role} header forwarded by the API Gateway and
 * populates the Spring Security context so that {@code @PreAuthorize}
 * method-level annotations can be evaluated.
 */
@Component
// HTTP Filter - Intercepts requests for cross-cutting concerns
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
public class HeaderRoleAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String role     = request.getHeader("X-User-Role");
        String userUuid = request.getHeader("X-User-UUID");

        if (role != null && !role.isBlank()) {
            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userUuid, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
