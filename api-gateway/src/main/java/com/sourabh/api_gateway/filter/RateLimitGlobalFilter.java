package com.sourabh.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final int REQUEST_LIMIT = 100;

    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitGlobalFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);
        String redisKey = "rate-limit:" + clientIp;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(currentCount -> {
                    Mono<Boolean> expiryUpdate = currentCount == 1
                            ? redisTemplate.expire(redisKey, REFILL_DURATION)
                            : Mono.just(Boolean.TRUE);

                    return expiryUpdate.then(Mono.defer(() -> {
                        if (currentCount <= REQUEST_LIMIT) {
                            return chain.filter(exchange);
                        }

                        log.warn("Rate limit exceeded for IP: {}", clientIp);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add(
                                "X-Rate-Limit-Retry-After-Seconds",
                                String.valueOf(REFILL_DURATION.toSeconds()));
                        return exchange.getResponse().setComplete();
                    }));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
