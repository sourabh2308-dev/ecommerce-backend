package com.sourabh.review_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign client configuration for the review-service.
 *
 * <p>Registers a global {@link RequestInterceptor} that attaches the shared
 * internal secret ({@code X-Internal-Secret} header) to every outbound Feign
 * request. This allows service-to-service calls (e.g.&nbsp;review-service
 * calling order-service to verify a purchase) to pass the
 * {@link InternalSecretFilter} on the receiving side without requiring a
 * user JWT.
 *
 * <p>The secret value is injected from the {@code internal.secret} application
 * property, which is typically supplied via environment variables in the
 * Docker Compose configuration or Config Server.
 *
 * @see InternalSecretFilter
 * @see com.sourabh.review_service.feign.OrderServiceClient
 */
@Configuration
public class FeignConfig {

    /**
     * Shared internal secret used for authenticating outbound
     * service-to-service Feign calls, loaded from the
     * {@code internal.secret} property.
     */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Creates a {@link RequestInterceptor} that appends the
     * {@code X-Internal-Secret} header to every outgoing Feign HTTP request.
     *
     * @return the configured {@link RequestInterceptor} bean
     */
    @Bean
    public RequestInterceptor feignOutboundSecretInterceptor() {
        return template -> template.header("X-Internal-Secret", internalSecret);
    }
}
