package com.sourabh.order_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that validates the {@code X-Internal-Secret} header on
 * every inbound request to ensure it originates from a trusted internal
 * microservice.
 *
 * <p>This filter is the inbound counterpart to the outbound
 * {@link FeignConfig#feignOutboundSecretInterceptor()} interceptor. When
 * the header is missing or does not match the configured secret, the
 * request is immediately rejected with HTTP 403 Forbidden.</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution
 * per request even if the request is forwarded internally.</p>
 *
 * @see FeignConfig
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    /** Expected secret value injected from application properties. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Compares the {@code X-Internal-Secret} request header against the
     * configured secret. If the values do not match, the filter short-circuits
     * the filter chain and returns a 403 status code.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
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