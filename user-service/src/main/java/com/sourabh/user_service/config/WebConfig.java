package com.sourabh.user_service.config;

import com.sourabh.user_service.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the user-service.
 *
 * <p>Registers application-level {@link org.springframework.web.servlet.HandlerInterceptor}
 * instances that execute around controller method invocations. The
 * {@link RoleInterceptor} is added here to enforce fine-grained role
 * checks on selected endpoints.</p>
 *
 * <p><b>Note:</b> The {@code InternalSecretFilter} functionality is
 * handled as a servlet filter (not an interceptor) so that it runs
 * before the Spring Security filter chain.</p>
 *
 * @see RoleInterceptor
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** Interceptor that validates role-based access on annotated handler methods. */
    private final RoleInterceptor roleInterceptor;

    /**
     * Registers the {@link RoleInterceptor} with the Spring MVC interceptor
     * registry so that it is applied to all incoming requests.
     *
     * @param registry the {@link InterceptorRegistry} provided by the framework
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor);
    }
}
