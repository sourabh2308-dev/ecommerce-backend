package com.sourabh.payment_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter that guards internal service-to-service endpoints by
 * validating the {@code X-Internal-Secret} request header against a shared
 * secret configured via {@code internal.secret} in application properties.
 *
 * <p>When the header is missing or does not match the expected value the
 * request is immediately rejected with HTTP 403 Forbidden.  This prevents
 * external clients from invoking endpoints that are intended exclusively
 * for inter-microservice communication (e.g. stock adjustment calls from
 * the order service).
 *
 * @see SecurityConfig
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    /** Shared secret loaded from {@code internal.secret} property. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Compares the {@code X-Internal-Secret} header value with the configured
     * secret.  On mismatch the response is short-circuited with 403.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain to invoke on success
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
