package com.org.userService.Utility;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

    public static String getUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getClaim("userId");
        if (userId == null) throw new RuntimeException("userId not found in JWT");
        return userId;
    }

    public static String getUsername() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = jwt.getSubject();
        if (username == null) throw new RuntimeException("username not found in JWT");
        return username;
    }
}
