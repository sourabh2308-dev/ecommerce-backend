package com.sourabh.auth_service.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the auth-service.
 *
 * <p>JWT validation is handled exclusively at the API Gateway.  This service
 * therefore permits all HTTP requests at the Spring Security level; actual
 * access control is enforced by {@link InternalSecretFilter}, which verifies
 * the {@code X-Internal-Secret} header before Spring Security runs.</p>
 *
 * <p>Key beans provided:</p>
 * <ul>
 *   <li>{@link UserDetailsService} &ndash; empty in-memory store (no local
 *       user DB).</li>
 *   <li>{@link SecurityFilterChain} &ndash; disables CSRF, form login, and
 *       HTTP Basic; permits all requests; returns plain 403 on auth
 *       failures.</li>
 *   <li>{@link PasswordEncoder} &ndash; BCrypt encoder used to verify user
 *       passwords fetched from {@code user-service}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Provides an empty {@link UserDetailsService}.  The auth-service does
     * not maintain a local user store; user lookups are performed against
     * {@code user-service} via REST.
     *
     * @return an empty {@link InMemoryUserDetailsManager}
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    /**
     * Configures the HTTP security filter chain.
     *
     * <ul>
     *   <li>CSRF protection disabled (stateless JWT architecture).</li>
     *   <li>Form login and HTTP Basic authentication disabled.</li>
     *   <li>All requests permitted (guarded by {@link InternalSecretFilter}).</li>
     *   <li>Authentication failures return HTTP 403 without a
     *       {@code WWW-Authenticate} header to avoid browser Basic-Auth
     *       popups.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * Provides a BCrypt-based {@link PasswordEncoder} used to compare
     * plain-text passwords against hashed values stored in
     * {@code user-service}.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
