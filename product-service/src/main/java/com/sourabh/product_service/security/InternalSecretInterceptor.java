package com.sourabh.product_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class InternalSecretInterceptor implements HandlerInterceptor {

    private final String expectedSecret;

    public InternalSecretInterceptor(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

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
