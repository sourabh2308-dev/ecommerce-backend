package com.sourabh.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that validates the {@code X-Internal-Secret}
 * header on internal-only endpoints (e.g. {@code /api/order/internal/**}).
 *
 * <p>This provides a lightweight authentication mechanism for
 * service-to-service communication that does not carry a JWT. The shared
 * secret is configured via the {@code internal.secret} property and must
 * match the value sent by the calling microservice.</p>
 *
 * <p>If the header is missing or does not match, the interceptor
 * short-circuits the request with HTTP 403 Forbidden.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Component
public class InternalSecretInterceptor implements HandlerInterceptor {

    /**
     * Expected shared secret, injected from the {@code internal.secret}
     * application property.
     */
    @Value("${internal.secret}")
    private String expectedSecret;

    /**
     * Validates the {@code X-Internal-Secret} header before the request
     * reaches the controller.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @param handler  the target handler (controller method)
     * @return {@code true} if the secret matches and the request should
     *         proceed; {@code false} to block the request with 403
     * @throws Exception if an unexpected error occurs
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String providedSecret =
                request.getHeader("X-Internal-Secret");

        if (providedSecret == null ||
                !providedSecret.equals(expectedSecret)) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
