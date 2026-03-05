package com.sourabh.product_service.filter;

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
 * Servlet filter that extracts (or generates) a correlation identifier for
 * every inbound HTTP request and stores it in the SLF4J
 * {@link MDC Mapped Diagnostic Context}.
 *
 * <p>If the API Gateway has already attached an {@code X-Correlation-Id}
 * header, that value is reused; otherwise a new random UUID is created.
 * The correlation ID is:
 * <ul>
 *   <li>Added to every log statement via the MDC key {@value #MDC_KEY}.</li>
 *   <li>Echoed back in the HTTP response header {@value #CORRELATION_ID_HEADER}
 *       so that callers can trace requests end-to-end.</li>
 * </ul>
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} to ensure the correlation ID
 * is available before any other filter or controller code executes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name carrying the correlation identifier. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Key under which the correlation ID is stored in the SLF4J MDC. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Reads or generates a correlation ID, places it into the MDC, attaches
     * it to the response header, and cleans up the MDC after the request
     * completes.
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
