package com.sourabh.order_service.filter;

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
 * Servlet filter that propagates a correlation identifier across the request
 * lifecycle to enable distributed tracing.
 *
 * <p>On each inbound request the filter checks for an {@code X-Correlation-Id}
 * header (typically set by the API Gateway). If the header is absent or blank,
 * a new random UUID is generated. The correlation ID is then:</p>
 * <ol>
 *   <li>Stored in the SLF4J {@link MDC} so it appears in every log statement
 *       produced during request processing.</li>
 *   <li>Echoed back in the HTTP response header so that upstream callers can
 *       correlate responses to their requests.</li>
 * </ol>
 *
 * <p>The filter runs at {@link Ordered#HIGHEST_PRECEDENCE} to ensure the
 * correlation ID is available before any other filter executes.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name used to transport the correlation identifier. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** MDC key under which the correlation ID is stored for log output. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Reads or generates a correlation ID, stores it in MDC, attaches it to
     * the response, and ensures the MDC entry is cleaned up after the request
     * completes.
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
