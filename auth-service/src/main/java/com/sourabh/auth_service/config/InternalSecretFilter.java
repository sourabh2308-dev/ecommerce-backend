package com.sourabh.auth_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that enforces service-to-service authentication by
 * requiring a valid {@code X-Internal-Secret} header on every inbound
 * request.
 *
 * <p>All traffic to the auth-service is expected to arrive through the
 * API Gateway, which attaches the shared secret header.  Requests missing
 * the header or carrying an incorrect value receive an HTTP 403 Forbidden
 * response and are not forwarded to downstream filters or controllers.</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution
 * per request even when the filter is registered in multiple chains.</p>
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    /** Shared secret loaded from {@code internal.secret} in application properties. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Compares the {@code X-Internal-Secret} request header against the
     * configured secret.  If the values do not match the filter
     * short-circuits the chain and returns 403 Forbidden.
     *
     * @param request      the incoming HTTP request
     * @param response     the HTTP response
     * @param filterChain  the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
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
