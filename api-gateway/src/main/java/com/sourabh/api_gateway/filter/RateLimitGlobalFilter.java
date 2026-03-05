package com.sourabh.api_gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global rate-limiting filter that enforces per-client request quotas at the
 * API Gateway level using the token-bucket algorithm.
 *
 * <h3>Algorithm</h3>
 * <p>Each unique client IP address is allocated a {@link Bucket} that holds up
 * to {@value #REQUEST_LIMIT} tokens. Tokens are refilled greedily every
 * {@code 1 minute}. When a request arrives, one token is consumed; if the
 * bucket is empty the client receives an {@code HTTP 429 Too Many Requests}
 * response along with an {@code X-Rate-Limit-Retry-After-Seconds} header.</p>
 *
 * <h3>Storage</h3>
 * <p>Buckets are currently stored in an in-memory {@link ConcurrentHashMap},
 * which is suitable for single-node deployments. A
 * {@link ReactiveStringRedisTemplate} is injected as a future upgrade path
 * for distributed rate-limit synchronisation across multiple gateway
 * replicas.</p>
 *
 * <h3>Filter Ordering</h3>
 * <p>Runs at {@code HIGHEST_PRECEDENCE + 1}, immediately after
 * {@link CorrelationIdGlobalFilter}, so rate-limited requests are rejected
 * before any expensive downstream processing (authentication, routing)
 * takes place.</p>
 *
 * @see CorrelationIdGlobalFilter
 */
@Component
@Slf4j
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    /** Maximum number of requests a single IP may issue per refill window. */
    private static final int REQUEST_LIMIT = 100;

    /** Duration of the token refill window. */
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    /** Per-IP token buckets; suitable for a single-node gateway deployment. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Reactive Redis template injected for future distributed rate-limit
     * synchronisation. Currently unused but wired for an upgrade path.
     */
    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Constructs the filter with the provided Redis template.
     *
     * @param redisTemplate reactive Redis string template for potential
     *                      distributed bucket storage
     */
    public RateLimitGlobalFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Applies rate limiting to the incoming request.
     *
     * <p>The client IP is resolved (respecting {@code X-Forwarded-For} when
     * present) and the corresponding token bucket is consulted. If a token is
     * available the request proceeds; otherwise a {@code 429} response is
     * returned immediately.</p>
     *
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next filter
     * @return {@link Mono} that completes when the downstream chain finishes
     *         or when the rate-limited response has been written
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);

        return Mono.fromCallable(() -> resolveBucket(clientIp).tryConsume(1))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    }
                    log.warn("Rate limit exceeded for IP: {}", clientIp);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", "60");
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * Returns the filter's execution order.
     *
     * <p>{@code HIGHEST_PRECEDENCE + 1} ensures this filter runs immediately
     * after the correlation-ID filter but before security and routing.</p>
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * Retrieves or creates the token bucket associated with the given client
     * IP address. Buckets are configured with a classic bandwidth limit using
     * greedy refill.
     *
     * @param clientIp the resolved client IP address
     * @return the {@link Bucket} for the specified IP
     */
    private Bucket resolveBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, ip -> {
            Bandwidth limit = Bandwidth.classic(
                    REQUEST_LIMIT,
                    Refill.greedy(REQUEST_LIMIT, REFILL_DURATION)
            );
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Resolves the originating client IP address from the exchange.
     *
     * <p>When the gateway sits behind a load balancer or reverse proxy the
     * real client IP is typically found in the {@code X-Forwarded-For} header
     * (the first comma-separated value). If the header is absent, the TCP
     * remote address of the connection is used instead.</p>
     *
     * @param exchange the current server exchange
     * @return the best-effort client IP address, or {@code "unknown"} if it
     *         cannot be determined
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
