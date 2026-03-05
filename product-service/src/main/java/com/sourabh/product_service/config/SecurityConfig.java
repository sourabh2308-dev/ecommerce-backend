package com.sourabh.product_service.config;

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
 * Spring Security configuration for the product-service.
 *
 * <p>JWT validation and user authentication are performed exclusively at the
 * API Gateway.  This service trusts the {@code X-User-Role} and
 * {@code X-User-UUID} headers injected by the gateway and uses
 * {@link HeaderRoleAuthenticationFilter} to populate the Spring Security
 * context so that {@code @PreAuthorize} expressions can be evaluated locally.
 *
 * <h3>Key decisions</h3>
 * <ul>
 *   <li>CSRF is disabled — the service is stateless and token-based.</li>
 *   <li>Form login and HTTP-basic are disabled — authentication is header-based.</li>
 *   <li>Session management is set to {@code STATELESS} — no server-side sessions.</li>
 *   <li>All request paths are {@code permitAll()} at the HTTP level; fine-grained
 *       access control is enforced via {@code @PreAuthorize} on individual
 *       controller methods.</li>
 * </ul>
 *
 * @see HeaderRoleAuthenticationFilter
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filter that converts gateway-injected role headers into a Spring Security authentication. */
    private final HeaderRoleAuthenticationFilter headerRoleAuthFilter;

    /**
     * Builds the {@link SecurityFilterChain} with stateless session management,
     * disabled CSRF / form-login / HTTP-basic, and registers the
     * {@link HeaderRoleAuthenticationFilter} before Spring's default
     * {@link UsernamePasswordAuthenticationFilter}.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring
     * @return the fully-configured {@code SecurityFilterChain}
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
