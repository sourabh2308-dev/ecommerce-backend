package com.sourabh.order_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for OpenFeign client customisation within the
 * order-service.
 *
 * <p>Registers a global {@link RequestInterceptor} bean that attaches the
 * {@code X-Internal-Secret} header to every outgoing Feign request. This
 * allows downstream microservices (e.g., product-service, user-service) to
 * verify that the call originated from a trusted internal service rather
 * than an external client.</p>
 *
 * @see com.sourabh.order_service.config.InternalSecretFilter
 */
@Configuration
public class FeignConfig {

    /** Shared secret used for service-to-service authentication, injected from application properties. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Creates a Feign {@link RequestInterceptor} that appends the
     * {@code X-Internal-Secret} header to every outbound Feign HTTP request.
     *
     * <p>The bean is explicitly named {@code feignOutboundSecretInterceptor}
     * to prevent naming collisions with the inbound
     * {@link InternalSecretFilter} component.</p>
     *
     * @return a {@link RequestInterceptor} that injects the internal secret header
     */
    @Bean
    public RequestInterceptor feignOutboundSecretInterceptor() {
        return template -> template.header("X-Internal-Secret", internalSecret);
    }
}
