package com.sourabh.product_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
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
public class InternalSecretFilter extends OncePerRequestFilter {

    @Value("${internal.secret}")
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("X-Internal-Secret");

        if (!internalSecret.equals(header)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}