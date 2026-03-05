package com.sourabh.user_service.config;

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
 * Servlet filter that extracts the {@code X-User-Role} and {@code X-User-UUID}
 * headers injected by the API Gateway and populates the Spring Security
 * {@link SecurityContextHolder} accordingly.
 *
 * <p>Because JWT validation is performed exclusively at the gateway layer,
 * this filter trusts the forwarded headers and converts them into a
 * {@link UsernamePasswordAuthenticationToken} with the appropriate
 * {@code ROLE_*} authority so that {@code @PreAuthorize} annotations on
 * controller methods can be evaluated.</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution
 * per request even when the request is forwarded internally.</p>
 *
 * @see SecurityConfig
 */
@Component
public class HeaderRoleAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Reads role and user UUID from gateway-injected headers and, when present,
     * sets the security context for the current request.
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
        String role     = request.getHeader("X-User-Role");
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
