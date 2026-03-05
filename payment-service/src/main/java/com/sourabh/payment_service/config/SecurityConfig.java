package com.sourabh.payment_service.config;

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
 * Spring Security configuration for the payment service.
 *
 * <p>JWT verification is performed upstream by the API Gateway; this service
 * trusts the forwarded {@code X-User-Role} header (processed by
 * {@link HeaderRoleAuthenticationFilter}) for method-level RBAC via
 * {@code @PreAuthorize} annotations on controller methods.
 *
 * <p><b>Key decisions:</b>
 * <ul>
 *   <li>CSRF disabled — stateless REST API behind an API Gateway.</li>
 *   <li>Session management set to {@code STATELESS} — no server-side
 *       sessions; every request is independently authenticated.</li>
 *   <li>Form login and HTTP Basic disabled — authentication is header-based.</li>
 *   <li>All request matchers are set to {@code permitAll()} at the HTTP level;
 *       fine-grained role checks happen via {@code @PreAuthorize} on each
 *       controller method.</li>
 * </ul>
 *
 * @see HeaderRoleAuthenticationFilter
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filter that extracts role/UUID from gateway headers into the SecurityContext. */
    private final HeaderRoleAuthenticationFilter headerRoleAuthFilter;

    /**
     * Builds the {@link SecurityFilterChain} for this service.
     *
     * <p>Registers {@link HeaderRoleAuthenticationFilter} before Spring's
     * default {@link UsernamePasswordAuthenticationFilter} so that the
     * security context is populated before authorisation decisions are made.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the fully configured security filter chain
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
