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
import com.org.therapistService.Entity.TherapistServicesDto;
import com.org.therapistService.Proxy.ClientServiceProxy;
import com.org.therapistService.Services.TherapistService;

@RestController
public class TherapistController {
	
	@Autowired
	private ClientServiceProxy clientServiceProxy;

	@Autowired
	private TherapistService therapistService;
	
	@GetMapping("/therapists")
	public List<TherapistDto> getAllTherapists(){
		return therapistService.getAllTherapists();
	}
	
	@PostMapping("/therapist/create")
	public void createTherapist(@RequestBody TherapistDto therapistDto) {
		therapistService.createTherapist(therapistDto);
	}
	
	@PostMapping("/{therapistId}/create-client")
	public ResponseEntity<String> createClient(
			@PathVariable String therapistId,
			@RequestBody ClientDto clientDto){
		
		String clientId = clientServiceProxy.createClient(clientDto);
		return ResponseEntity.ok(clientId);
	}
	
	@PostMapping("/therapist/create-service")
	public void createTherapistService(@RequestBody TherapistServicesDto therapistServicesDto) {
		therapistService.createTherapistServices(therapistServicesDto);
	}
	
	@PostMapping("/therapist/create-appointment")
	public void createTherapistAppointment(@RequestBody TherapistDto therapistDto) {
		therapistService.createTherapist(therapistDto);
	}
	
	@PostMapping("/therapist/create-availability-rules")
	public void createTherapistAvailabilityRules(@RequestBody TherapistDto therapistDto) {
		therapistService.createTherapist(therapistDto);
	}
	
	@PostMapping("/therapist/create-availability-overrides")
	public void createTherapistAvailabilityOverrides(@RequestBody TherapistDto therapistDto) {
		therapistService.createTherapist(therapistDto);
	}
}
