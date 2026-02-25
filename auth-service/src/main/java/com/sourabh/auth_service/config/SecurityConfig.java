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
 * Auth-service security config.
 *
 * JWT validation is handled exclusively at the API Gateway.
 * This service only needs to:
 *   1. Permit all requests (gateway forwards with X-Internal-Secret, checked
 *      by InternalSecretFilter before Spring Security runs).
 *   2. Provide a PasswordEncoder bean for BCrypt operations.
 *
 * @EnableMethodSecurity enables @PreAuthorize annotations on controller methods.
 */
// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(ex -> ex
                        // Return plain 403 — never emit WWW-Authenticate which triggers browser Basic-Auth popup
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // InternalSecretFilter guards all paths
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
