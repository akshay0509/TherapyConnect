package com.org.therapistService.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.org.therapistService.Entity.ClientDto;
import com.org.therapistService.Entity.TherapistAppointmentsDto;
import com.org.therapistService.Entity.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Entity.TherapistAvailabilityRulesDto;
import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Entity.TherapistServicesDto;
import com.org.therapistService.Proxy.ClientServiceProxy;
import com.org.therapistService.Services.AvailabilitySlotGeneratorService;
import com.org.therapistService.Services.TherapistService;

@RestController
public class TherapistController {
	
	@Autowired
	private ClientServiceProxy clientServiceProxy;

	@Autowired
	private TherapistService therapistService;
	
	@Autowired
    private AvailabilitySlotGeneratorService availabilitySlotGeneratorService;
	
	//get all therapists
	@GetMapping("/therapists")
	public List<TherapistDto> getAllTherapists(){
		return therapistService.getAllTherapists();
	}
	
	//create a therapist
	@PostMapping("/therapist/create-therapist")
	public void createTherapist(@RequestBody TherapistDto therapistDto) {
		therapistService.createTherapist(therapistDto);
	}
	
	//create a client for a therapist
	@PostMapping("/{therapistId}/create-client")
	public ResponseEntity<String> createClient(
			@PathVariable String therapistId,
			@RequestBody ClientDto clientDto){
		
		String clientId = clientServiceProxy.createClient(clientDto);
		return ResponseEntity.ok(clientId);
	}
	
	//get all therapist services
	@GetMapping("/therapist-services")
	public List<TherapistServicesDto> getAllTherapistServices(){
		return therapistService.getAllTherapistServices();
	}
	
	//get all services for a therapist
	@GetMapping("/therapist-services/{therapistId}")
	public List<TherapistServicesDto> getTherapistServices(@PathVariable String therapistId){
		return therapistService.getTherapistServices(therapistId);
	}
	
	//create a therapist service
	@PostMapping("/therapist/create-service")
	public void createTherapistService(@RequestBody TherapistServicesDto therapistServicesDto) {
		therapistService.createTherapistServices(therapistServicesDto);
	}
	
	//get a therapist availability
	@GetMapping("/therapist-availability/{therapistId}")
	public void getTherapistAvailability(@PathVariable String therapistId) {
		therapistService.getTherapistAvailability(therapistId);
	}
	
	@PostMapping("/therapist/create-appointment")
	public void createTherapistAppointment(@RequestBody TherapistAppointmentsDto therapistAppointmentsDto) {
		therapistService.createTherapistAppointments(therapistAppointmentsDto);
	}
	
	@PostMapping("/therapist/create-availability-rules")
	public void createTherapistAvailabilityRules(@RequestBody TherapistAvailabilityRulesDto therapistAvailabilityRulesDto) {
		therapistService.createTherapistAvailabilityRules(therapistAvailabilityRulesDto);
	}
	
	@PostMapping("/therapist/create-availability-overrides")
	public void createTherapistAvailabilityOverrides(@RequestBody TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) {
		therapistService.createTherapistAvailabilityOverrides(therapistAvailabilityOverridesDto);
	}
	
	@PostMapping("/_generate")
    public void generateSlots(
            @PathVariable String therapistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        availabilitySlotGeneratorService.generateTherapistAvailabilitySlots(therapistId, from, to);
    }
}
