package com.sourabh.product_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that validates inter-service requests by inspecting the
 * {@code X-Internal-Secret} header.
 *
 * <p>Every inbound request must carry an {@code X-Internal-Secret} header
 * whose value matches the configured {@code internal.secret} property
 * (typically injected from environment variables or a config server).
 * Requests with a missing or incorrect secret are rejected immediately
 * with HTTP {@code 403 Forbidden}.
 *
 * <p>This filter extends {@link OncePerRequestFilter} to guarantee a
 * single execution per request even when the request is forwarded
 * internally.
 *
 * @see SecurityConfig
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    /** Shared secret expected in the {@code X-Internal-Secret} request header. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Validates the {@code X-Internal-Secret} header and either continues the
     * filter chain or short-circuits with a {@code 403} response.
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