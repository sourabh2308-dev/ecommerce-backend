package com.sourabh.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that guards internal (service-to-service) endpoints by
 * validating the {@code X-Internal-Secret} request header against a shared
 * secret configured via {@code internal.secret} in application properties.
 *
 * <p>If the header is missing or does not match the expected value, the
 * request is immediately rejected with HTTP 403 (Forbidden). Otherwise
 * the filter chain continues normally.</p>
 *
 * <p>This filter is registered as a Spring component and extends
 * {@link OncePerRequestFilter} to ensure idempotent execution per
 * request.</p>
 *
 * @see SecurityConfig
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    /** Shared secret injected from {@code internal.secret} configuration property. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Validates the {@code X-Internal-Secret} header on every request.
     *
     * <p>Returns HTTP 403 immediately when the header value does not match
     * the configured secret; otherwise delegates to the next filter in the
     * chain.</p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to invoke
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