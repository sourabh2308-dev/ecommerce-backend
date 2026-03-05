package com.sourabh.auth_service.filter;

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
 * Servlet filter that propagates or generates an {@code X-Correlation-Id}
 * header for end-to-end distributed tracing.
 *
 * <p>If the API Gateway (or any upstream caller) includes an
 * {@code X-Correlation-Id} header, this filter stores its value in the
 * SLF4J {@link MDC} so every log statement produced during the request
 * automatically includes the correlation ID.  If no header is present a
 * new UUID is generated.</p>
 *
 * <p>The correlation ID is also attached to the HTTP response, allowing
 * callers to match requests to responses when debugging.</p>
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so the ID is available to
 * all subsequent filters and controller logic.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name carrying the correlation ID. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** MDC key under which the correlation ID is stored for logging. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Extracts or generates a correlation ID, places it in the MDC and on
     * the response header, then delegates to the next filter.  The MDC
     * entry is removed in a {@code finally} block to prevent leaking to
     * subsequent requests served by the same thread.
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
