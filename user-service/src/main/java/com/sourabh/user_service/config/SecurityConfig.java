package com.sourabh.user_service.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the user-service.
 *
 * <p>JWT validation is performed exclusively at the API Gateway. The gateway
 * forwards trusted {@code X-User-Role} / {@code X-User-UUID} headers which
 * {@link HeaderRoleAuthenticationFilter} converts into a Spring Security
 * authentication token, enabling {@code @PreAuthorize} evaluation on
 * controller methods.</p>
 *
 * <h3>Key decisions</h3>
 * <ul>
 *   <li>CSRF disabled &ndash; the service is stateless and consumed via
 *       bearer tokens only.</li>
 *   <li>Sessions disabled (STATELESS) &ndash; no server-side session store.</li>
 *   <li>All requests are permitted at the filter-chain level; method-level
 *       security ({@code @PreAuthorize}) controls authorisation per endpoint.</li>
 * </ul>
 *
 * @see HeaderRoleAuthenticationFilter
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filter that populates the security context from gateway-injected headers. */
    private final HeaderRoleAuthenticationFilter headerRoleAuthFilter;

    /**
     * Defines the HTTP security filter chain for incoming requests.
     *
     * <p>Disables form login, HTTP Basic, and CSRF; enforces stateless sessions;
     * and registers the {@link HeaderRoleAuthenticationFilter} before the
     * default {@link UsernamePasswordAuthenticationFilter}.</p>
     *
     * @param http the {@link HttpSecurity} DSL builder
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if configuration fails
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

    /**
     * Provides a BCrypt {@link PasswordEncoder} bean used to hash and verify
     * user passwords during registration and password-change flows.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
