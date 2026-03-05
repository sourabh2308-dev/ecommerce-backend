package com.sourabh.product_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that validates an internal shared-secret header
 * on service-to-service requests.
 * <p>
 * <strong>Note:</strong> This interceptor has been superseded by
 * {@code InternalSecretFilter} (a servlet filter) and is no longer
 * registered in {@code WebConfig}. It is retained for reference only.
 * </p>
 * <p>
 * When active, it inspects the {@code X-Internal-Secret} header on every
 * incoming request. If the header is missing or does not match the
 * expected secret, the request is rejected with HTTP 403 Forbidden.
 * </p>
 */
public class InternalSecretInterceptor implements HandlerInterceptor {

    /** The secret value that incoming requests must present. */
    private final String expectedSecret;

    /**
     * Constructs the interceptor with the expected shared secret.
     *
     * @param expectedSecret the secret string that callers must supply
     *                       via the {@code X-Internal-Secret} header
     */
    public InternalSecretInterceptor(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    /**
     * Validates the {@code X-Internal-Secret} header before the request
     * reaches the controller.
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response (set to 403 on failure)
     * @param handler  the target handler
     * @return {@code true} if the secret matches and the request may proceed;
     *         {@code false} otherwise (response is set to 403)
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
