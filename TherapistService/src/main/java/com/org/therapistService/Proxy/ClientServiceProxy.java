package com.org.therapistService.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.org.therapistService.Entity.ClientDto;

@Service
public class ClientServiceProxy {

	@Autowired
	private RestTemplate restTemplate;
	
	public Long createClient(ClientDto clientDto) {
		String url = "http://client-service/client";
		ResponseEntity<Long> response = restTemplate.postForEntity(url, clientDto, Long.class);
		return response.getBody();
	}
}
