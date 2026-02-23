package com.sourabh.api_gateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements
        org.springframework.security.authentication.ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication)
            throws AuthenticationException {

        String token = authentication.getCredentials().toString();

        try {
            Claims claims = jwtUtil.validateToken(token);

            String role = claims.get("role", String.class);
            String uuid = claims.get("uuid", String.class);
            String email = claims.getSubject();

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );
            // Store uuid so ClaimsForwardingFilter can read it without re-parsing
            auth.setDetails(uuid);

            return Mono.just(auth);

        } catch (Exception e) {
            // Return error so Spring Security properly sends 401 Unauthorized
            return Mono.error(new BadCredentialsException("Invalid or expired JWT token"));
        }
    }
}
