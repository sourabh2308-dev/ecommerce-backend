package com.sourabh.order_service.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the order-service.
 *
 * <p>JWT validation is handled at the API Gateway; this service trusts the
 * {@code X-User-Role} header forwarded by the gateway for role-based access
 * control via {@code @PreAuthorize} annotations on controller/service methods.</p>
 *
 * <p>Key configuration decisions:</p>
 * <ul>
 *   <li>CSRF protection is disabled (stateless REST API).</li>
 *   <li>Form login and HTTP Basic are disabled (token-based auth).</li>
 *   <li>Session management is set to {@link SessionCreationPolicy#STATELESS}.</li>
 *   <li>All requests are permitted at the filter chain level; fine-grained
 *       access control is enforced at the method level with {@code @PreAuthorize}.</li>
 *   <li>The {@link HeaderRoleAuthenticationFilter} is registered before
 *       {@link UsernamePasswordAuthenticationFilter} to populate the
 *       {@link org.springframework.security.core.context.SecurityContextHolder}.</li>
 * </ul>
 *
 * @see HeaderRoleAuthenticationFilter
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filter that reads gateway-forwarded role/UUID headers into the security context. */
    private final HeaderRoleAuthenticationFilter headerRoleAuthFilter;

    /**
     * Builds the {@link SecurityFilterChain} bean that defines the HTTP
     * security policy for the service.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(headerRoleAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
