package com.org.gatewayService.Proxy;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.org.gatewayService.Dto.AuthRequest;
import com.org.gatewayService.Dto.AuthResponse;

@Service
public class UserServiceProxy {

	@Autowired
	private RestTemplate restTemplate;

	private final String userServiceBaseUrl = "http://user-service";

	public AuthResponse validateUser(AuthRequest authRequest) {

		String url = userServiceBaseUrl + "/validate-user";
		System.out.println("POST REST call to user service= "+authRequest);
		AuthResponse authResponse = restTemplate.postForObject(url, authRequest, AuthResponse.class);
		System.out.println("response from POST REST CALL "+authResponse);
		return authResponse;
		 
		/*
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<AuthRequest> requestEntity = new HttpEntity<>(authRequest, headers);

		ResponseEntity<AuthResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, AuthResponse.class);

		return response.getBody();
		*/
	}
	
	public void forgotPassword(Map<String, String> request) {
		restTemplate.postForObject(userServiceBaseUrl + "/forgot-password", request, String.class);
	}

	public void resetPassword(Map<String, String> request) {
		restTemplate.postForObject(userServiceBaseUrl + "/reset-password", request, String.class);
	}

}
