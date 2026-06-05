package com.ecommerce.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            UserContext.setUserId(Long.parseLong(userId));
        }
        String role = request.getHeader("X-User-Role");
        if (role != null && !role.isBlank()) {
            UserContext.setRole(role);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
