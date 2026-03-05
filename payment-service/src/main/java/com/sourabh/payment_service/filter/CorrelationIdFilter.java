package com.sourabh.payment_service.filter;

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
 * Servlet filter that propagates the distributed tracing correlation ID
 * through the SLF4J Mapped Diagnostic Context (MDC).
 *
 * <p>If the API Gateway supplies an {@code X-Correlation-Id} header, that
 * value is reused; otherwise a new UUID is generated.  The correlation ID
 * is:
 * <ul>
 *   <li>Stored in the MDC under key {@value #MDC_KEY} so that every log
 *       statement emitted during the request automatically includes it.</li>
 *   <li>Echoed back to the caller via the {@code X-Correlation-Id}
 *       response header for end-to-end traceability.</li>
 * </ul>
 *
 * <p>Registered at {@link Ordered#HIGHEST_PRECEDENCE} so the correlation
 * ID is available to all downstream filters and the controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name used to carry the correlation ID. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** SLF4J MDC key under which the correlation ID is stored. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Reads or generates the correlation ID, places it in MDC and the
     * response header, then proceeds with the filter chain.  The MDC
     * entry is always removed in the {@code finally} block to prevent
     * thread-pool contamination.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain
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
