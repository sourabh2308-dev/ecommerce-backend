package com.sourabh.user_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Picks up the {@code X-Correlation-Id} header set by the API Gateway and
 * stores it in SLF4J MDC so it appears in every log statement for the request.
 *
 * <p>Also attaches the header to the HTTP response so callers can trace
 * requests end-to-end.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.addHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
