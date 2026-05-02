package com.lpu.groupservice.config;

import org.springframework.stereotype.Component;

import com.lpu.groupservice.exception.CustomException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class UserContext {

    public Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new CustomException("Unauthorized: X-User-Id header missing. Please provide a valid JWT via the API Gateway.");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new CustomException("Invalid X-User-Id header value: " + userId);
        }
    }

    public String getRole(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        return role == null ? "" : role;
    }
}
