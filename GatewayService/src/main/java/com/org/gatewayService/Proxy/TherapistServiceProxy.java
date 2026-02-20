package com.org.gatewayService.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TherapistServiceProxy {

	@Autowired
	private RestTemplate restTemplate;
	
	private final String therapistServiceBaseUrl = "http://therapist-service";
	
	public String getTherapistId(String userId) {
		
		String url = therapistServiceBaseUrl + "/internal/therapist/user/" + userId;
		String therapistId = restTemplate.getForObject(url, String.class);
		return therapistId;
	}
}
