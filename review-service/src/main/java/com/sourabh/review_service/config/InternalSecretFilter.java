package com.sourabh.review_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that validates the {@code X-Internal-Secret} header on every
 * incoming request before the request reaches the controller layer.
 *
 * <p>This filter is designed for internal (service-to-service) communication
 * where standard JWT-based authentication is not applicable. When another
 * microservice (e.g.&nbsp;order-service or product-service) calls review-service
 * endpoints, it must include the shared secret in the {@code X-Internal-Secret}
 * header. If the header is missing or does not match the configured value the
 * request is immediately rejected with HTTP {@code 403 Forbidden}.
 *
 * <p>Extends {@link OncePerRequestFilter} to ensure exactly-once execution
 * per request regardless of internal forwards or dispatches.
 *
 * @see FeignConfig
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    /**
     * Expected secret loaded from the {@code internal.secret} application
     * property, supplied via environment variables or Config Server.
     */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Compares the {@code X-Internal-Secret} request header against the
     * expected value. If they do not match, the response status is set to
     * {@code 403 Forbidden} and the filter chain is short-circuited so that
     * the request never reaches the controller.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to invoke on success
     * @throws ServletException if a servlet error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
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
