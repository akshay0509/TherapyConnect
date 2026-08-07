package com.org.gatewayService.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.org.gatewayService.Exception.TherapistLookupUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class TherapistServiceProxy {

	private static final Logger logger = LoggerFactory.getLogger(TherapistServiceProxy.class);

	@Autowired
	private RestTemplate restTemplate;
	
	private final String therapistServiceBaseUrl = "http://therapist-service";

	/**
	 * Returns the therapist id, or null when the user genuinely has no profile —
	 * the endpoint answers 200 with an empty body in that case.
	 *
	 * Never returns null to mean "I could not find out". That is the whole point:
	 * a null here is a fact about the user, not about the network.
	 */
	@CircuitBreaker(name = "therapistService", fallbackMethod = "getTherapistIdFallback")
	public String getTherapistId(String userId) {
		String url = therapistServiceBaseUrl + "/internal/therapist/user/" + userId;
		return restTemplate.getForObject(url, String.class);
	}

	/**
	 * Signals UNKNOWN rather than answering "no profile" on the service's behalf.
	 * Returning null here is what sent existing therapists to the create-profile
	 * page whenever TherapistService was restarting.
	 */
	public String getTherapistIdFallback(String userId, Throwable t) {
		logger.error("TherapistService lookup unavailable for userId={}: {}", userId, t.getMessage());
		throw new TherapistLookupUnavailableException(
				"Could not determine therapist profile for userId=" + userId, t);
	}
}
