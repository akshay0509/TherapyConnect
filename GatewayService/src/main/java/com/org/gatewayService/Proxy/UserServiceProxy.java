package com.org.gatewayService.Proxy;

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
		AuthResponse authResponse = restTemplate.postForObject(url, authRequest, AuthResponse.class);
		return authResponse;
	}

}
