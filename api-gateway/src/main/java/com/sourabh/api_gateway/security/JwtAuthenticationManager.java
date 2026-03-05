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

/**
 * Reactive authentication manager that validates JWT tokens and produces a
 * fully populated {@link Authentication} object for Spring Security.
 *
 * <h3>Validation Flow</h3>
 * <ol>
 *   <li>Receives the raw JWT string (set as the credential by
 *       {@link JwtAuthenticationConverter}).</li>
 *   <li>Delegates to {@link JwtUtil#validateToken(String)} which verifies the
 *       signature and expiration.</li>
 *   <li>Extracts the {@code role}, {@code uuid}, and {@code email} (subject)
 *       claims from the token.</li>
 *   <li>Builds a {@link UsernamePasswordAuthenticationToken} with:
 *       <ul>
 *         <li><strong>Principal</strong> &mdash; the user's email address.</li>
 *         <li><strong>Authorities</strong> &mdash; a single
 *             {@code ROLE_&lt;role&gt;} granted authority.</li>
 *         <li><strong>Details</strong> &mdash; the user's UUID, stored for
 *             downstream filters that need it without re-parsing the JWT.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>If the token is invalid or expired, a {@link BadCredentialsException} is
 * returned so Spring Security responds with {@code 401 Unauthorized}.</p>
 *
 * @see JwtAuthenticationConverter
 * @see JwtUtil
 * @see SecurityConfig
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements
        org.springframework.security.authentication.ReactiveAuthenticationManager {

    /** Utility for cryptographic JWT validation and claim extraction. */
    private final JwtUtil jwtUtil;

    /**
     * Validates the JWT token carried in the supplied {@link Authentication}
     * and returns a fully authenticated token on success.
     *
     * @param authentication an unauthenticated token whose
     *                       {@link Authentication#getCredentials()} contains
     *                       the raw JWT string
     * @return a {@link Mono} emitting a fully authenticated
     *         {@link UsernamePasswordAuthenticationToken}, or an error signal
     *         with {@link BadCredentialsException} if validation fails
     * @throws AuthenticationException propagated by the reactive pipeline
     */
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
            auth.setDetails(uuid);

            return Mono.just(auth);

        } catch (Exception e) {
            return Mono.error(new BadCredentialsException("Invalid or expired JWT token"));
        }
    }
}
