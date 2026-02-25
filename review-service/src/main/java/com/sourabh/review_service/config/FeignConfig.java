package com.sourabh.review_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring Configuration - Defines beans and infrastructure setup
@Configuration
public class FeignConfig {

    @Value("${internal.secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor feignOutboundSecretInterceptor() {
        return template -> template.header("X-Internal-Secret", internalSecret);
    }
}
