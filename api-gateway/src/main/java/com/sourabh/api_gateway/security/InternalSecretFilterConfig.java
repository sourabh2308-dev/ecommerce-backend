package com.sourabh.api_gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Configuration
public class InternalSecretFilterConfig {

    @Value("${internal_secret}")
    private String internalSecret;

    @Bean
    public GlobalFilter internalSecretFilter() {
        return (exchange, chain) -> {

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-Internal-Secret", internalSecret)
                    .build();

            return chain.filter(
                    exchange.mutate().request(mutatedRequest).build()
            );
        };
    }
}