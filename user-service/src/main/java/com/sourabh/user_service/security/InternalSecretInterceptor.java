package com.sourabh.user_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that guards internal (service-to-service)
 * endpoints by verifying the {@code X-Internal-Secret} request header.
 * <p>
 * Requests that omit the header or supply an incorrect secret are
 * immediately rejected with HTTP 403 Forbidden.  This mechanism
 * replaces JWT authentication for inter-service calls that are
 * not initiated by end-users.
 * </p>
 *
 * @see org.springframework.web.servlet.config.annotation.InterceptorRegistry
 */
@Component
public class InternalSecretInterceptor implements HandlerInterceptor {

    /** The expected secret value, injected from application configuration. */
    @Value("${internal.secret}")
    private String expectedSecret;

    /**
     * Validates the {@code X-Internal-Secret} header before the controller
     * method executes.
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response (set to 403 on failure)
     * @param handler  the target handler
     * @return {@code true} if the secret matches and the request may proceed;
     *         {@code false} otherwise
     * @throws Exception if an unexpected error occurs
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String providedSecret = request.getHeader("X-Internal-Secret");

        if (providedSecret == null || !providedSecret.equals(expectedSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
