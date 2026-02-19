package com.sourabh.review_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalSecretInterceptor implements HandlerInterceptor {

    @Value("${internal.secret}")
    private String expectedSecret;

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
