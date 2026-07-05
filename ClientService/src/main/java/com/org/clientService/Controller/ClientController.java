package com.org.clientService.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Dto.ClientStatusUpdateRequest;
import com.org.clientService.Services.ClientService;
import com.org.clientService.Utility.SecurityUtils;

@RestController
public class ClientController {

	private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

	@Autowired
	private ClientService clientService;

	@GetMapping("/get/{clientId}")
	public ClientDto getClients(@PathVariable String clientId){
		String therapistId = SecurityUtils.getTherapistId();
		return clientService.getClient(therapistId, clientId);
	}

	@PostMapping("/create-client")
	public ResponseEntity<String> createClient(@RequestBody ClientDto clientDto) {
		// tenant id always comes from the JWT, never from the request body —
		// otherwise any authenticated caller could create clients under
		// another therapist
		clientDto.setTherapistId(SecurityUtils.getTherapistId());

		String clientId;
		try {
			clientId = clientService.createClient(clientDto);
		} catch (JsonProcessingException e) {
			logger.error("Failed to create client", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create client.");
		}
		return ResponseEntity.ok(clientId);
	}

	@PutMapping("/update/{clientId}")
	public ResponseEntity<ClientDto> updateClient(@PathVariable String clientId, @RequestBody ClientDto clientDto) throws JsonProcessingException {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(clientService.updateClient(therapistId, clientId, clientDto));
	}

	@PatchMapping("/update-status/{clientId}")
	public ResponseEntity<ClientDto> updateClientStatus(@PathVariable String clientId, @RequestBody ClientStatusUpdateRequest request) throws JsonProcessingException {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(clientService.updateClientStatus(therapistId, clientId, request.getStatus()));
	}

}
