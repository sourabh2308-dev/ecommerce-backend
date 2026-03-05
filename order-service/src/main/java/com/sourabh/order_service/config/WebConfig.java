package com.sourabh.order_service.config;

import com.sourabh.order_service.security.InternalSecretInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration class that registers custom
 * {@link org.springframework.web.servlet.HandlerInterceptor} instances
 * with the application's request processing pipeline.
 *
 * <p>Currently registers the {@link InternalSecretInterceptor} to protect
 * all {@code /api/orders/**} endpoints, ensuring only requests carrying a
 * valid internal secret header are processed.</p>
 *
 * @see InternalSecretInterceptor
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** Interceptor that validates the {@code X-Internal-Secret} header on inbound requests. */
    private final InternalSecretInterceptor internalSecretInterceptor;

    /**
     * Registers the {@link InternalSecretInterceptor} to intercept all
     * requests matching the {@code /api/orders/**} URL pattern.
     *
     * @param registry the {@link InterceptorRegistry} provided by Spring MVC
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(internalSecretInterceptor)
                .addPathPatterns("/api/orders/**");
    }
}
