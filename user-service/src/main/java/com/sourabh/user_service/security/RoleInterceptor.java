package com.sourabh.user_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that enforces role-based access control on
 * admin endpoints.
 * <p>
 * Inspects the {@code X-User-Role} header (set by the API gateway after
 * JWT validation) and rejects non-ADMIN users with HTTP 403 Forbidden
 * when the request URI contains {@code /admin/}.
 * </p>
 *
 * @see InternalSecretInterceptor
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    /**
     * Checks the caller's role before the controller method executes.
     * <p>
     * Only requests whose URI includes {@code /admin/} are subjected
     * to the role check; all other paths pass through unconditionally.
     * </p>
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response (set to 403 on failure)
     * @param handler  the target handler
     * @return {@code true} if the request is authorised to proceed;
     *         {@code false} otherwise
     * @throws Exception if an unexpected error occurs
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String role = request.getHeader("X-User-Role");

        if (request.getRequestURI().contains("/admin/")) {

            if (role == null || !role.equals("ADMIN")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }

        return true;
    }
}
