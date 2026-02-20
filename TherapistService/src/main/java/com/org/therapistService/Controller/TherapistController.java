package com.org.therapistService.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.therapistService.Entity.ClientDto;
import com.org.therapistService.Entity.TherapistAppointmentsDto;
import com.org.therapistService.Entity.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Entity.TherapistAvailabilityRulesDto;
import com.org.therapistService.Entity.TherapistClientsDto;
import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Entity.TherapistServicesDto;
import com.org.therapistService.Proxy.ClientServiceProxy;
import com.org.therapistService.Services.AvailabilitySlotService;
import com.org.therapistService.Services.TherapistService;
import com.org.therapistService.Utility.SecurityUtils;

@RestController
public class TherapistController {

	@Autowired
	private ClientServiceProxy clientServiceProxy;

	@Autowired
	private TherapistService therapistService;

	@Autowired
	private AvailabilitySlotService availabilitySlotService;

	//get all therapists
	@GetMapping("/therapists")
	public List<TherapistDto> getAllTherapists(){
		return therapistService.getAllTherapists();
	}
	
	@GetMapping("/therapistProfile")
	public TherapistDto getTherapistProfile(){
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getTherapist(therapistId);
	}

	//get all therapist services
	@GetMapping("/therapist-services")
	public List<TherapistServicesDto> getAllTherapistServices(){
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getTherapistServices(therapistId);
	}

	//get all services for a therapist
	@GetMapping("{therapistId}/therapist-services")
	public List<TherapistServicesDto> getTherapistServices(@PathVariable String therapistId){
		return therapistService.getTherapistServices(therapistId);
	}

	//get a therapist availability
	@GetMapping("{therapistId}/therapist-availability")
	public void getTherapistAvailability(@PathVariable String therapistId) {
		therapistService.getTherapistAvailability(therapistId);
	}

	//create a therapist
	@PostMapping("/create-therapist")
	public void createTherapist(@RequestBody TherapistDto therapistDto) {
		therapistService.createTherapist(therapistDto);
	}

	//create a client for a therapist
	@PostMapping("/create-client")
	public ResponseEntity<String> createClient(@RequestBody ClientDto clientDto){
		String therapistId = SecurityUtils.getTherapistId();
		clientDto.setTherapistId(therapistId);
		String clientId = clientServiceProxy.createClient(clientDto);
		System.out.println("printing the client ID: "+clientId);
		String clientName = clientDto.getFirstName() + " " + clientDto.getLastName();
		therapistService.addClient(therapistId, clientId, clientName);
		return ResponseEntity.ok(clientId);
	}
	
	@GetMapping("/clients")
	public ResponseEntity<List<TherapistClientsDto>> getClients() {
		String therapistId = SecurityUtils.getTherapistId();
		List<TherapistClientsDto> therapistClientsDtoList = therapistService.getClientsForTherapist(therapistId);
		return ResponseEntity.ok(therapistClientsDtoList);
	}

	//create a therapist service
	@PostMapping("/create-service")
	public void createTherapistService(@RequestBody TherapistServicesDto therapistServicesDto) {
		String therapistId = SecurityUtils.getTherapistId();
		therapistServicesDto.setTherapistId(therapistId);
		therapistService.createTherapistServices(therapistServicesDto);
	}

	@PostMapping("/create-appointment")
	public void createTherapistAppointment(@RequestBody TherapistAppointmentsDto therapistAppointmentsDto) {
		therapistService.createTherapistAppointments(therapistAppointmentsDto);
	}
	
	@GetMapping("/availability-rules")
	public List<TherapistAvailabilityRulesDto> getTherapistAvailabilityRules() {
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getAllTherapistAvailabilityRules(therapistId);
	}

	@PostMapping("/create-availability-rules")
	public void createTherapistAvailabilityRules(@RequestBody List<TherapistAvailabilityRulesDto> therapistAvailabilityRulesDtoList) {
		String therapistId = SecurityUtils.getTherapistId();
		for (TherapistAvailabilityRulesDto therapistAvailabilityRulesDto : therapistAvailabilityRulesDtoList) {
			therapistAvailabilityRulesDto.setTherapistId(therapistId);
	    }
		therapistService.createTherapistAvailabilityRules(therapistAvailabilityRulesDtoList);
	}

	@PostMapping("/create-availability-overrides")
	public void createTherapistAvailabilityOverrides(@RequestBody TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) {
		therapistService.createTherapistAvailabilityOverrides(therapistAvailabilityOverridesDto);
	}	

	@PostMapping("{therapistId}/generate-slots")
	public ResponseEntity<String> generateSlots(@PathVariable String therapistId,
												@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
												@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		
		if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body("End date cannot be before start date.");
        }

		try {
			availabilitySlotService.generateAvailabilitySlots(therapistId, startDate, endDate);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		return ResponseEntity.ok(String.format("Successfully generated slots"));
	}
	
	@DeleteMapping("{therapistId}/delete-slots")
	public ResponseEntity<String> deleteSlots(@PathVariable String therapistId,
											  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
											  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){
		
		if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body("End date cannot be before start date.");
        }
		
		try {
			availabilitySlotService.deleteAvailabilitySlots(therapistId, startDate, endDate);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		
		return ResponseEntity.ok(String.format("Successfully deleted slots"));
	}
}
