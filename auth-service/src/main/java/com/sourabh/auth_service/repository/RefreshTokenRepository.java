package com.sourabh.auth_service.repository;

import com.sourabh.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RefreshToken} entities.
 *
 * <p>Provides standard CRUD operations plus a derived query method for
 * looking up refresh tokens by their unique token string.  Spring Data
 * auto-generates the SQL implementation from the method name at
 * startup.</p>
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its unique token value.
     *
     * @param token the UUID token string
     * @return an {@link Optional} containing the token entity, or empty if
     *         no matching token exists
     */
    Optional<RefreshToken> findByToken(String token);
}
