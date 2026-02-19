package com.sourabh.api_gateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements org.springframework.security.authentication.ReactiveAuthenticationManager {

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

            Authentication auth =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            return Mono.just(auth);

        } catch (Exception e) {
            return Mono.empty();
        }
    }
}
