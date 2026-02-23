package com.sourabh.order_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Adds X-Internal-Secret to every outbound Feign request so
     * downstream services' InternalSecretFilter accepts the call.
     * Named 'feignOutboundSecretInterceptor' to avoid collision with the
     * inbound InternalSecretInterceptor security @Component.
     */
    @Bean
    public RequestInterceptor feignOutboundSecretInterceptor() {
        return template -> template.header("X-Internal-Secret", internalSecret);
    }
}
