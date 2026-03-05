package com.sourabh.order_service.config;

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
 * {@link SecurityContextHolder} with an authenticated principal.
 *
 * <p>JWT validation is performed at the gateway level; this downstream
 * service trusts the forwarded headers and converts them into a
 * {@link UsernamePasswordAuthenticationToken} with the appropriate
 * {@code ROLE_*} granted authority. This enables method-level security
 * annotations such as {@code @PreAuthorize("hasRole('ADMIN')")} to function
 * correctly.</p>
 *
 * <p>The filter extends {@link OncePerRequestFilter} to guarantee single
 * execution per request regardless of the filter chain configuration.</p>
 *
 * @see SecurityConfig
 */
@Component
public class HeaderRoleAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Extracts user identity and role from gateway-forwarded headers and
     * establishes a Spring Security authentication context for the current
     * request.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to invoke
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
