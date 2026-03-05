package com.sourabh.payment_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servlet filter that converts the {@code X-User-Role} and {@code X-User-UUID}
 * headers — injected by the API Gateway after JWT validation — into a Spring
 * Security {@link UsernamePasswordAuthenticationToken}.
 *
 * <p>By populating the {@link SecurityContextHolder} with an authentication
 * object that carries the appropriate {@code ROLE_*} granted authority,
 * downstream {@code @PreAuthorize} annotations on controller methods are
 * evaluated correctly without the payment service having to perform its own
 * JWT verification.
 *
 * <p><b>Filter chain position:</b> Registered before
 * {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}
 * in {@link SecurityConfig#securityFilterChain}.
 *
 * @see SecurityConfig
 */
@Component
public class HeaderRoleAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Extracts role and user UUID from gateway-forwarded headers and
     * establishes the Spring Security context for the current request.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain to invoke
     * @throws ServletException if an error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String role = request.getHeader("X-User-Role");
        String userUuid = request.getHeader("X-User-UUID");

        if (role != null && !role.isBlank()) {
            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userUuid, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
