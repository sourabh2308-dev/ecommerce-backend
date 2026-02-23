package com.sourabh.product_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC interceptor registration for product-service.
 *
 * InternalSecretInterceptor is intentionally removed — its function is
 * fully covered by the InternalSecretFilter servlet filter which applies
 * to all request paths before reaching Spring MVC.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // No interceptors needed — InternalSecretFilter handles gateway validation
}
