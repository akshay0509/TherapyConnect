package com.org.gatewayService.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.events.login.LoginFailureEvent;
import com.org.events.login.LoginSuccessEvent;
import com.org.gatewayService.Dto.AuthRequest;
import com.org.gatewayService.Dto.AuthResponse;
import com.org.gatewayService.Entity.RefreshTokens;
import com.org.gatewayService.Messaging.LoginEventProducer;
import com.org.gatewayService.Proxy.TherapistServiceProxy;
import com.org.gatewayService.Proxy.UserServiceProxy;
import com.org.gatewayService.Services.RefreshTokensService;
import com.org.gatewayService.Utility.JwtUtil;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private UserServiceProxy userServiceProxy;

	@Autowired
	private TherapistServiceProxy therapistServiceProxy;

	@Autowired
    private JwtUtil jwtUtil;

	@Autowired
	private LoginEventProducer loginEventProducer;

	@Autowired
	private RefreshTokensService refreshTokensService;

	@PostMapping("/login")
	@RateLimiter(name = "loginRateLimiter", fallbackMethod = "loginRateLimitFallback")
	public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequest authRequest, HttpServletRequest httpRequest) {
		AuthResponse authResponse = userServiceProxy.validateUser(authRequest);

		if(!authResponse.isAuthenticated()) {

			LoginFailureEvent loginFailureEvent = new LoginFailureEvent();
			loginFailureEvent.setUserId(authResponse.getUserId());
			loginFailureEvent.setUsername(authRequest.getUsername());
			loginFailureEvent.setIpAddress(httpRequest.getRemoteAddr());
			loginFailureEvent.setUserAgent(httpRequest.getHeader("User-Agent"));
			loginFailureEvent.setTimestamp(Instant.now());
			loginFailureEvent.setReason(authResponse.getFailureReason().name());

			loginEventProducer.publishLoginFailure(loginFailureEvent);
			return ResponseEntity.status(401).body(Map.of("failureReason", authResponse.getFailureReason().name()));
		}

		LoginSuccessEvent loginSuccessEvent = new LoginSuccessEvent();
		loginSuccessEvent.setUserId(authResponse.getUserId());
		loginSuccessEvent.setUsername(authRequest.getUsername());
		loginSuccessEvent.setIpAddress(httpRequest.getRemoteAddr());
		loginSuccessEvent.setUserAgent(httpRequest.getHeader("User-Agent"));
		loginSuccessEvent.setTimestamp(Instant.now());

        loginEventProducer.publishLoginSuccess(loginSuccessEvent);

		String therapistId = therapistServiceProxy.getTherapistId(authResponse.getUserId());

		String accessToken = jwtUtil.generateToken(
				authRequest.getUsername(),
				List.of("read", "write"),
				authResponse.getRoles(),
				authResponse.getUserId(),
				therapistId);

		RefreshTokens refreshToken = refreshTokensService.createRefreshToken(
				authRequest.getUsername(), authResponse.getUserId(), therapistId, authResponse.getRoles());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken.getToken()).toString())
				.body(Map.of("token", accessToken));
	}

	public ResponseEntity<Map<String, String>> loginRateLimitFallback(
			AuthRequest authRequest, HttpServletRequest httpRequest, Throwable t) {
		logger.warn("Login rate limit exceeded from {}", httpRequest.getRemoteAddr());
		return ResponseEntity.status(429).body(Map.of("error", "Too many login attempts. Please try again later."));
	}

	@PostMapping("/refresh")
	public ResponseEntity<Map<String, String>> refresh(HttpServletRequest httpRequest) {
		String tokenValue = extractRefreshTokenCookie(httpRequest);
		if (tokenValue == null) {
			return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
		}

		Optional<RefreshTokens> tokenOpt = refreshTokensService.findByToken(tokenValue);
		if (tokenOpt.isEmpty() || refreshTokensService.isExpiredOrRevoked(tokenOpt.get())) {
			return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
		}

		RefreshTokens stored = tokenOpt.get();
		Set<String> roles = refreshTokensService.getRoles(stored);

		// If therapistId was null at login time (new user pre-setup), try to fetch it now
		String therapistId = stored.getTherapistId();
		if (therapistId == null) {
			therapistId = therapistServiceProxy.getTherapistId(stored.getUserId());
		}

		// Rotate: delete old token, issue new one preserving roles (and updated therapistId)
		RefreshTokens newToken = refreshTokensService.createRefreshToken(
				stored.getUsername(), stored.getUserId(), therapistId, roles);

		String accessToken = jwtUtil.generateToken(
				stored.getUsername(),
				List.of("read", "write"),
				roles,
				stored.getUserId(),
				therapistId);

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(newToken.getToken()).toString())
				.body(Map.of("token", accessToken));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
		String tokenValue = extractRefreshTokenCookie(httpRequest);
		if (tokenValue != null) {
			refreshTokensService.findByToken(tokenValue)
					.ifPresent(t -> refreshTokensService.revokeByUsername(t.getUsername()));
		}

		ResponseCookie expiredCookie = ResponseCookie.from("refresh_token", "")
				.httpOnly(true)
				.secure(true)
				.path("/auth/refresh")
				.maxAge(0)
				.sameSite("None")
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
				.build();
	}

	@PostMapping("/forgot-password")
	public Map<String, String> forgotPassword(@RequestBody Map<String, String> request) {
		userServiceProxy.forgotPassword(request);
		return Map.of("message", "If the email exists, reset instructions will be sent.");
	}

	@PostMapping("/reset-password")
	public Map<String, String> resetPassword(@RequestBody Map<String, String> request) {
		userServiceProxy.resetPassword(request);
		return Map.of("message", "Password reset successfully.");
	}

	private ResponseCookie buildRefreshCookie(String tokenValue) {
		return ResponseCookie.from("refresh_token", tokenValue)
				.httpOnly(true)
				.secure(true)
				.path("/auth/refresh")
				.maxAge(7 * 24 * 60 * 60)
				.sameSite("None")
				.build();
	}

	private String extractRefreshTokenCookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;
		for (Cookie cookie : request.getCookies()) {
			if ("refresh_token".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
