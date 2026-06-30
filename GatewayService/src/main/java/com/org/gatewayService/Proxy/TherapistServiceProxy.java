package com.org.gatewayService.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class TherapistServiceProxy {

	private static final Logger logger = LoggerFactory.getLogger(TherapistServiceProxy.class);

	@Autowired
	private RestTemplate restTemplate;
	
	private final String therapistServiceBaseUrl = "http://therapist-service";

	@CircuitBreaker(name = "therapistService", fallbackMethod = "getTherapistIdFallback")
	public String getTherapistId(String userId) {
		String url = therapistServiceBaseUrl + "/internal/therapist/user/" + userId;
		return restTemplate.getForObject(url, String.class);
	}

	public String getTherapistIdFallback(String userId, Throwable t) {
		logger.error("TherapistService circuit breaker open for userId={}: {}", userId, t.getMessage());
		return null;
	}
}
