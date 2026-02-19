package com.sourabh.user_service.config;

import com.sourabh.user_service.security.InternalSecretInterceptor;
import com.sourabh.user_service.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;
    private final InternalSecretInterceptor internalSecretInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(internalSecretInterceptor)
                .addPathPatterns("/api/users/**");

        registry.addInterceptor(roleInterceptor);
    }
}

