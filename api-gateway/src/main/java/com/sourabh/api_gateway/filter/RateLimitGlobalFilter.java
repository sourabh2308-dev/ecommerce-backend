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
 * Global rate-limiting filter for the API Gateway.
 *
 * <p>Implements a token bucket algorithm using Bucket4j with an in-memory
 * store (one bucket per client IP). Each IP is allowed 100 requests per
 * minute. Requests exceeding the limit receive HTTP 429 Too Many Requests.
 *
 * <p>The Redis reactive template is injected as an optional future upgrade
 * path for distributed bucket synchronisation across multiple gateway replicas.
 */
@Component
@Slf4j
// HTTP Filter - Intercepts requests for cross-cutting concerns
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final int REQUEST_LIMIT = 100;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    /** Per-IP token buckets — suitable for a single-node gateway deployment. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitGlobalFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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

    @Override
    public int getOrder() {
        // Run early in the filter chain, before routing
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Bucket resolveBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, ip -> {
            Bandwidth limit = Bandwidth.classic(
                    REQUEST_LIMIT,
                    Refill.greedy(REQUEST_LIMIT, REFILL_DURATION)
            );
            return Bucket.builder().addLimit(limit).build();
        });
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        // Prefer X-Forwarded-For for requests passing through a load balancer
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
