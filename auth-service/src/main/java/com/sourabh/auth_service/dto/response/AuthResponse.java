package com.sourabh.auth_service.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after a successful login or token refresh.
 *
 * <p>Contains a short-lived JWT access token, a long-lived refresh token
 * (UUID), and the token type ({@code "Bearer"}) for use in the HTTP
 * {@code Authorization} header.</p>
 */
@Getter
@Builder
public class AuthResponse {

    /** JWT access token used to authenticate API requests. */
    private String accessToken;

    /** Opaque UUID refresh token used to obtain new token pairs. */
    private String refreshToken;

    /** Token scheme &ndash; always {@code "Bearer"}. */
    private String tokenType;
}
