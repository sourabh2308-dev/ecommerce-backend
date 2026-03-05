package com.sourabh.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global gateway filter that ensures every request carries a unique correlation
 * identifier for end-to-end distributed tracing.
 *
 * <h3>Behaviour</h3>
 * <ol>
 *   <li>Reads the incoming {@code X-Correlation-Id} header. If the header is
 *       absent or blank, a new UUID is generated.</li>
 *   <li>Forwards the correlation ID to the downstream service by mutating the
 *       outgoing request headers.</li>
 *   <li>Echoes the same value back to the caller in the response headers so
 *       clients can correlate their request with server-side logs.</li>
 *   <li>Stores the ID in the Reactor {@link reactor.util.context.Context} so
 *       reactive operators further down the chain can access it.</li>
 * </ol>
 *
 * <h3>Filter Ordering</h3>
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so the correlation ID is
 * available to every subsequent filter (including rate limiting, security,
 * and routing).</p>
 *
 * <p>Downstream servlet-based services pick up this header via their own
 * {@code CorrelationIdFilter} and place it into the SLF4J MDC for structured
 * logging.</p>
 *
 * @see RateLimitGlobalFilter
 */
@Component
@Slf4j
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    /** HTTP header name used to carry the correlation identifier. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Key used when storing the correlation ID in the SLF4J MDC or Reactor Context. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Intercepts each exchange to ensure a correlation ID is present.
     *
     * <p>If the incoming request already contains a valid
     * {@code X-Correlation-Id} header the value is reused; otherwise a fresh
     * UUID v4 is generated. The ID is then propagated to both the downstream
     * request and the client response.</p>
     *
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next filter
     * @return {@link Mono} that completes when the downstream chain finishes
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ctx.put(MDC_KEY, finalCorrelationId));
    }

    /**
     * Returns the filter's execution order.
     *
     * <p>{@link Ordered#HIGHEST_PRECEDENCE} guarantees this filter runs before
     * all other global filters, including {@link RateLimitGlobalFilter}.</p>
     *
     * @return the highest-precedence order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
