package com.org.clientService.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Entity.ClientDto;
import com.org.clientService.Services.ClientService;
import com.org.clientService.Utility.SecurityUtils;

@RestController
public class ClientController {

	@Autowired
	private ClientService clientService;
	
	@GetMapping("/get/{clientId}")
	public ClientDto getClients(@PathVariable String clientId){
		String therapistId = SecurityUtils.getTherapistId();
		return clientService.getClient(therapistId, clientId);
	}
	
	@PostMapping("/create-client")
	public ResponseEntity<String> createClient(@RequestBody ClientDto clientDto) {
		String clientId;
		try {
			clientId = clientService.createClient(clientDto);
		} catch (JsonProcessingException e) {
			return ResponseEntity.ok("failed");
		}
		return ResponseEntity.ok(clientId);
	}
}
