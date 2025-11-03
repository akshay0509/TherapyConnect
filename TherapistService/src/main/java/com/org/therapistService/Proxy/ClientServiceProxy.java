package com.org.therapistService.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.org.therapistService.Entity.ClientDto;

@Service
public class ClientServiceProxy {

	@Autowired
	private RestTemplate restTemplate;
	
	private final String clientServiceBaseUrl = "http://client-service";
	
	public String createClient(ClientDto clientDto) {
		String url = clientServiceBaseUrl + "/client";
		String response = restTemplate.postForObject(url, clientDto, String.class);
		return response;
	}
	
	public ClientDto getClient(String clientId) {
		String url = clientServiceBaseUrl + "/client/{clientId}";
		ClientDto response = restTemplate.getForObject(url, ClientDto.class, clientId);
		return response;
	}
}
