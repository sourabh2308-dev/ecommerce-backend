package com.sourabh.review_service.filter;

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
 * Servlet filter that propagates a correlation identifier across the entire
 * request lifecycle for distributed tracing.
 *
 * <p>If the incoming request contains an {@code X-Correlation-Id} header
 * (typically set by the API Gateway) the value is reused; otherwise a new
 * random UUID is generated. The identifier is:
 * <ol>
 *   <li>Placed into the SLF4J {@link MDC} so it appears in every log line
 *       produced during the request (key: {@value #MDC_KEY}).</li>
 *   <li>Attached to the HTTP response header so downstream consumers
 *       and the original caller can correlate responses with requests.</li>
 * </ol>
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} to ensure the correlation
 * ID is available to all subsequent filters and interceptors.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Name of the HTTP header carrying the correlation identifier. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Key under which the correlation ID is stored in the SLF4J MDC. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Reads or generates the correlation ID, stores it in the MDC, echoes
     * it on the response, and cleans up the MDC after the filter chain
     * completes.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response (correlation header is added)
     * @param filterChain the remaining filter chain to invoke
     * @throws ServletException if a servlet error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
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
