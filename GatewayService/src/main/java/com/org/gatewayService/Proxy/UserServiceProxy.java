package com.org.gatewayService.Proxy;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.org.gatewayService.Dto.AuthRequest;
import com.org.gatewayService.Dto.AuthResponse;
import com.org.gatewayService.Enum.FailureReason;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class UserServiceProxy {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceProxy.class);

	@Autowired
	private RestTemplate restTemplate;

	private final String userServiceBaseUrl = "http://user-service";

	@CircuitBreaker(name = "userService", fallbackMethod = "validateUserFallback")
	public AuthResponse validateUser(AuthRequest authRequest) {
		String url = userServiceBaseUrl + "/validate-user";
		return restTemplate.postForObject(url, authRequest, AuthResponse.class);
	}

	public AuthResponse validateUserFallback(AuthRequest authRequest, Throwable t) {
		logger.error("UserService circuit breaker open: {}", t.getMessage());
		AuthResponse fallback = new AuthResponse();
		fallback.setAuthenticated(false);
		fallback.setFailureReason(FailureReason.SERVICE_UNAVAILABLE);
		return fallback;
	}

	public void forgotPassword(Map<String, String> request) {
		restTemplate.postForObject(userServiceBaseUrl + "/forgot-password", request, String.class);
	}

	public void resetPassword(Map<String, String> request) {
		restTemplate.postForObject(userServiceBaseUrl + "/reset-password", request, String.class);
	}
}
