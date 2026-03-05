package com.sourabh.product_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the product-service.
 *
 * <p>Implements {@link WebMvcConfigurer} to allow future registration of
 * interceptors, message converters, or CORS mappings.  Currently no
 * interceptors are registered because gateway-level request validation is
 * handled entirely by {@link InternalSecretFilter}.
 *
 * @see InternalSecretFilter
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
