package com.sourabh.review_service.config;

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
 * <p>JWT validation has already been performed at the gateway level, so this
 * filter trusts the forwarded headers and converts them into a Spring Security
 * {@link UsernamePasswordAuthenticationToken}. This enables {@code @PreAuthorize}
 * annotations on {@link com.sourabh.review_service.controller.ReviewController}
 * to evaluate role-based access control (BUYER, SELLER, ADMIN).
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee exactly-once execution
 * per request, even when the request is internally forwarded or dispatched
 * multiple times by the servlet container.
 *
 * @see com.sourabh.review_service.controller.ReviewController
 */
@Component
public class HeaderRoleAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Extracts user identity and role from gateway-forwarded headers and
     * sets the {@link UsernamePasswordAuthenticationToken} in the current
     * {@link SecurityContextHolder}.
     *
     * <p>If the {@code X-User-Role} header is absent or blank the filter
     * passes the request along without setting any authentication, which
     * effectively makes the request anonymous.
     *
     * @param request     the incoming HTTP request containing forwarded headers
     * @param response    the HTTP response (not modified by this filter)
     * @param filterChain the remaining filter chain to invoke
     * @throws ServletException if a servlet error occurs during filtering
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
