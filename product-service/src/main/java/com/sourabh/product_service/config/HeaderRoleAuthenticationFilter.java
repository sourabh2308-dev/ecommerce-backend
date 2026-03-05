package com.sourabh.product_service.config;

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
 * Servlet filter that reads the {@code X-User-Role} and {@code X-User-UUID}
 * headers forwarded by the API Gateway and populates the Spring Security
 * {@link SecurityContextHolder} accordingly.
 *
 * <p>Once the security context is established, downstream controllers can
 * use {@code @PreAuthorize("hasRole('SELLER')")} and similar SpEL
 * expressions to enforce role-based access control without performing JWT
 * validation locally (JWT validation is handled exclusively at the gateway).
 *
 * <p>Roles are stored with the {@code ROLE_} prefix expected by Spring
 * Security (e.g. {@code ROLE_ADMIN}, {@code ROLE_SELLER}).
 *
 * @see SecurityConfig
 */
@Component
public class HeaderRoleAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Extracts the user role and UUID from gateway-injected headers, creates
     * a {@link UsernamePasswordAuthenticationToken} and stores it in the
     * current {@link SecurityContextHolder}.
     *
     * <p>If the {@code X-User-Role} header is absent or blank the request
     * proceeds unauthenticated — downstream endpoint security annotations
     * will decide whether to allow or deny access.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
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
