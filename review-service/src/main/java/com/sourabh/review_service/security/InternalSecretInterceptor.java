package com.sourabh.review_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC {@link HandlerInterceptor} that validates the
 * {@code X-Internal-Secret} header on incoming requests intended for
 * internal (service-to-service) endpoints.
 *
 * <p>Unlike the servlet-level
 * {@link com.sourabh.review_service.config.InternalSecretFilter}, this
 * interceptor operates within the Spring MVC dispatch pipeline and can
 * be selectively registered on specific URL patterns via a
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurer}.
 *
 * <p>If the provided header is absent or does not match the expected
 * secret the interceptor short-circuits the handler chain and returns
 * HTTP {@code 403 Forbidden}.
 *
 * @see com.sourabh.review_service.config.InternalSecretFilter
 */
@Component
public class InternalSecretInterceptor implements HandlerInterceptor {

    /**
     * Expected shared secret loaded from the {@code internal.secret}
     * application property.
     */
    @Value("${internal.secret}")
    private String expectedSecret;

    /**
     * Validates the {@code X-Internal-Secret} header before the request
     * reaches the controller method.
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response (set to 403 on failure)
     * @param handler  the handler about to be invoked
     * @return {@code true} if the secret matches and the request may
     *         proceed; {@code false} otherwise
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
