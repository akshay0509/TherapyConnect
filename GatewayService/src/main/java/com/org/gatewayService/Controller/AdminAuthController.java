package com.org.gatewayService.Controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.gatewayService.Utility.ClientIpUtil;
import com.org.gatewayService.Utility.JwtUtil;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AdminAuthController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthController.class);

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/admin-login")
    @RateLimiter(name = "adminLoginRateLimiter", fallbackMethod = "adminLoginRateLimitFallback")
    public ResponseEntity<Map<String, String>> adminLogin(@RequestBody Map<String, String> credentials,
            HttpServletRequest httpRequest) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (!constantTimeEquals(adminUsername, username) || !constantTimeEquals(adminPassword, password)) {
            logger.warn("Failed admin login attempt for username: {} from {}", username, ClientIpUtil.resolve(httpRequest));
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateAdminToken(username);
        logger.info("Admin login successful from {}", ClientIpUtil.resolve(httpRequest));
        return ResponseEntity.ok(Map.of("token", token));
    }

    public ResponseEntity<Map<String, String>> adminLoginRateLimitFallback(
            Map<String, String> credentials, HttpServletRequest httpRequest, Throwable t) {
        logger.warn("Admin login rate limit exceeded from {}", ClientIpUtil.resolve(httpRequest));
        return ResponseEntity.status(429).body(Map.of("error", "Too many login attempts. Please try again later."));
    }

    // String.equals short-circuits on the first differing character, which
    // leaks timing information on a credential check
    private boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
