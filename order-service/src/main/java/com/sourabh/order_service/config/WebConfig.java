package com.sourabh.order_service.config;

import com.sourabh.order_service.security.InternalSecretInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final InternalSecretInterceptor internalSecretInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(internalSecretInterceptor)
                .addPathPatterns("/api/orders/**");
    }
}
