package com.sourabh.auth_service.security;

import com.sourabh.auth_service.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Servlet filter that extracts and validates a JWT bearer token from the
 * {@code Authorization} header and populates the Spring Security context.
 *
 * <p>If the request contains a valid {@code Bearer <token>} header, the
 * filter parses the JWT via {@link JwtUtil}, extracts the user's email and
 * role, and sets a {@link UsernamePasswordAuthenticationToken} in the
 * {@link SecurityContextHolder}.  This enables {@code @PreAuthorize}
 * annotations on downstream controllers to perform role-based checks.</p>
 *
 * <p>Requests without a bearer token pass through unauthenticated,
 * allowing public endpoints to function normally.  Invalid tokens are
 * logged and silently ignored.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Utility for JWT parsing, validation, and claims extraction. */
    private final JwtUtil jwtUtil;

    /**
     * Inspects the {@code Authorization} header for a bearer token.  If
     * present and valid the user's email and role are placed into the
     * Spring Security context; otherwise the request continues
     * unauthenticated.
     *
     * @param request      the incoming HTTP request
     * @param response     the HTTP response
     * @param filterChain  the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            if (jwtUtil.validateToken(token)) {

                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + role)
                                )
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
