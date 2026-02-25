package com.sourabh.user_service.config;

import com.sourabh.user_service.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC interceptor registration.
 *
 * InternalSecretInterceptor is intentionally removed — its function is
 * covered by InternalSecretFilter (a servlet filter that runs before Spring
 * Security and applies to all request paths).
 */
// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor);
    }
}

