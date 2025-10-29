package com.org.therapistService.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.org.therapistService.Entity.ClientDto;
import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Proxy.ClientServiceProxy;
import com.org.therapistService.Services.TherapistService;

@RestController
public class TherapistController {
	
	@Autowired
	private ClientServiceProxy clientServiceProxy;

	@Autowired
	private TherapistService clientService;
	
	@GetMapping("/therapists")
	public List<TherapistDto> getAllTherapists(){
		return clientService.getAllTherapists();
	}
	
	@PostMapping("/therapist")
	public void createTherapist(@RequestBody TherapistDto therapistDto) {
		clientService.createTherapist(therapistDto);
	}
	
	@PostMapping("/{therapistId}/create-client")
	public ResponseEntity<Long> createClient(
			@PathVariable Long therapistId,
			@RequestBody ClientDto clientDto){
		
		Long clientId = clientServiceProxy.createClient(clientDto);
		return ResponseEntity.ok(clientId);
	}
}
