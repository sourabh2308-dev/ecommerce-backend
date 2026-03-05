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
 * Servlet filter that propagates a correlation identifier through the
 * request lifecycle for distributed tracing.
 *
 * <p>If the incoming request carries an {@code X-Correlation-Id} header
 * (typically injected by the API Gateway), that value is reused; otherwise
 * a new random UUID is generated. The correlation ID is:</p>
 * <ul>
 *   <li>Stored in the SLF4J {@link MDC} under key {@value #MDC_KEY} so that
 *       every log statement emitted during the request includes it.</li>
 *   <li>Added to the HTTP response header so that callers can correlate
 *       responses back to their requests.</li>
 * </ul>
 *
 * <p>Registered with {@link Ordered#HIGHEST_PRECEDENCE} to ensure it runs
 * before all other filters.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name used to carry the correlation identifier. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** MDC key under which the correlation ID is stored for log output. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Extracts or generates a correlation ID, places it in the MDC and the
     * response header, then delegates to the rest of the filter chain.
     *
     * <p>The MDC entry is always removed in a {@code finally} block to
     * prevent leaking into pooled threads.</p>
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
