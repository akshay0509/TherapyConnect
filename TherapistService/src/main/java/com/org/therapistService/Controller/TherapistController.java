package com.org.therapistService.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.therapistService.Dto.BulkAvailabilityOverridesRequest;
import com.org.therapistService.Dto.ClientDto;
import com.org.therapistService.Dto.ClientNotesDto;
import com.org.therapistService.Dto.DashboardStatsDto;
import com.org.therapistService.Dto.EarningsSessionDto;
import com.org.therapistService.Dto.EarningsSummaryDto;
import com.org.therapistService.Dto.PageResponseDto;
import com.org.therapistService.Dto.SessionDetailsDto;
import com.org.therapistService.Dto.SessionNotesDto;
import com.org.therapistService.Dto.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Dto.TherapistAvailabilityRulesDto;
import com.org.therapistService.Dto.TherapistClientsDto;
import com.org.therapistService.Dto.TherapistDto;
import com.org.therapistService.Dto.TherapistServicesDto;
import com.org.therapistService.Dto.TherapyDeliveryModeDto;
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

	@GetMapping("/dashboard/stats")
	public ResponseEntity<DashboardStatsDto> getDashboardStats() {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getDashboardStats(therapistId));
	}

	@GetMapping("/earnings/summary")
	public ResponseEntity<EarningsSummaryDto> getEarningsSummary() {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getEarningsSummary(therapistId));
	}

	@GetMapping("/earnings/sessions")
	public ResponseEntity<List<EarningsSessionDto>> getEarningsSessions(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String serviceId,
			@RequestParam(required = false) String modeId) {
		if (toDate.isBefore(fromDate)) {
			return ResponseEntity.badRequest().build();
		}
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getEarningsSessions(therapistId, fromDate, toDate, serviceId, modeId));
	}

	@GetMapping("/earnings/export")
	public ResponseEntity<byte[]> exportEarnings(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String serviceId,
			@RequestParam(required = false) String modeId) {
		if (toDate.isBefore(fromDate)) {
			return ResponseEntity.badRequest().build();
		}
		String therapistId = SecurityUtils.getTherapistId();
		byte[] csv = therapistService.exportEarningsCsv(therapistId, fromDate, toDate, serviceId, modeId);
		String filename = "earnings-" + fromDate + "-to-" + toDate + ".csv";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(csv);
	}

	//get all therapist services
	@GetMapping("/therapist-services")
	public List<TherapistServicesDto> getAllTherapistServices(){
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getTherapistServices(therapistId);
	}

	@GetMapping(value = "/therapist-services", params = {"page", "size"})
	public PageResponseDto<TherapistServicesDto> getAllTherapistServicesPage(@RequestParam int page, @RequestParam int size) {
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getTherapistServices(therapistId, page, size);
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
		String userId = SecurityUtils.getUserId();
		therapistService.createTherapist(therapistDto, userId);
	}

	//create a client for a therapist
	@PostMapping("/create-client")
	public ResponseEntity<String> createClient(@RequestBody ClientDto clientDto){
		String therapistId = SecurityUtils.getTherapistId();
		clientDto.setTherapistId(therapistId);
		String clientId = clientServiceProxy.createClient(clientDto);
		String clientName = clientDto.getFirstName() + " " + clientDto.getLastName();
		therapistService.addClient(therapistId, clientId, clientName, Boolean.TRUE.equals(clientDto.getDsf()));
		return ResponseEntity.ok(clientId);
	}

	@GetMapping("/clients")
	public ResponseEntity<List<TherapistClientsDto>> getClients() {
		String therapistId = SecurityUtils.getTherapistId();
		List<TherapistClientsDto> therapistClientsDtoList = therapistService.getClientsForTherapist(therapistId);
		return ResponseEntity.ok(therapistClientsDtoList);
	}

	@GetMapping(value = "/clients", params = {"page", "size"})
	public ResponseEntity<PageResponseDto<TherapistClientsDto>> getClientsPage(@RequestParam int page, @RequestParam int size) {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getClientsForTherapist(therapistId, page, size));
	}

	//create a therapist service
	@PostMapping("/create-service")
	public void createTherapistService(@RequestBody TherapistServicesDto therapistServicesDto) {
		String therapistId = SecurityUtils.getTherapistId();
		therapistServicesDto.setTherapistId(therapistId);
		therapistService.createTherapistServices(therapistServicesDto);
	}

	@PutMapping("/update-service/{serviceId}")
	public ResponseEntity<TherapistServicesDto> updateTherapistService(@PathVariable String serviceId, @RequestBody TherapistServicesDto therapistServicesDto) {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.updateTherapistService(therapistId, serviceId, therapistServicesDto));
	}

	@DeleteMapping("/delete-service/{serviceId}")
	public ResponseEntity<String> deleteTherapistService(@PathVariable String serviceId) {
		String therapistId = SecurityUtils.getTherapistId();
		therapistService.deleteTherapistService(therapistId, serviceId);
		return ResponseEntity.ok("Therapist service deleted successfully.");
	}

	@PostMapping("/delivery-modes")
	public ResponseEntity<TherapyDeliveryModeDto> createDeliveryMode(@RequestBody TherapyDeliveryModeDto dto) throws JsonProcessingException{
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.createDeliveryMode(therapistId, dto));
	}

	@GetMapping("/delivery-modes")
	public ResponseEntity<List<TherapyDeliveryModeDto>> getDeliveryModes(
			@RequestParam(required = false) String serviceId) {
		String therapistId = SecurityUtils.getTherapistId();
		if (serviceId != null) {
			return ResponseEntity.ok(therapistService.getDeliveryModesForService(therapistId, serviceId));
		}
		return ResponseEntity.ok(therapistService.getDeliveryModes(therapistId));
	}

	@PutMapping("/delivery-modes/{modeId}")
	public ResponseEntity<TherapyDeliveryModeDto> updateDeliveryMode(
			@PathVariable String modeId,
			@RequestBody TherapyDeliveryModeDto dto) throws JsonProcessingException{
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.updateDeliveryMode(therapistId, modeId, dto));
	}

	@DeleteMapping("/delivery-modes/{modeId}")
	public ResponseEntity<String> deleteDeliveryMode(@PathVariable String modeId) throws JsonProcessingException{
		String therapistId = SecurityUtils.getTherapistId();
		therapistService.deleteDeliveryMode(therapistId, modeId);
		return ResponseEntity.ok("Delivery mode deleted successfully.");
	}

	@GetMapping("/availability-rules")
	public List<TherapistAvailabilityRulesDto> getTherapistAvailabilityRules() {
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getAllTherapistAvailabilityRules(therapistId);
	}

	@GetMapping(value = "/availability-rules", params = {"page", "size"})
	public PageResponseDto<TherapistAvailabilityRulesDto> getTherapistAvailabilityRulesPage(@RequestParam int page, @RequestParam int size) {
		String therapistId = SecurityUtils.getTherapistId();
		return therapistService.getAllTherapistAvailabilityRules(therapistId, page, size);
	}

	@PostMapping("/create-availability-rules")
	public void createTherapistAvailabilityRules(@RequestBody List<TherapistAvailabilityRulesDto> therapistAvailabilityRulesDtoList) {
		String therapistId = SecurityUtils.getTherapistId();
		for (TherapistAvailabilityRulesDto therapistAvailabilityRulesDto : therapistAvailabilityRulesDtoList) {
			therapistAvailabilityRulesDto.setTherapistId(therapistId);
		}
		therapistService.createTherapistAvailabilityRules(therapistAvailabilityRulesDtoList);
	}

	@DeleteMapping("/availability-rules/{ruleId}")
	public ResponseEntity<String> deleteAvailabilityRule(@PathVariable String ruleId) {
		String therapistId = SecurityUtils.getTherapistId();
		therapistService.deleteAvailabilityRule(therapistId, ruleId);
		return ResponseEntity.ok("Availability rule deleted successfully.");
	}

	@PutMapping("/availability-rules/{ruleId}")
	public ResponseEntity<TherapistAvailabilityRulesDto> updateAvailabilityRule(@PathVariable String ruleId, @RequestBody TherapistAvailabilityRulesDto dto) {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.updateAvailabilityRule(therapistId, ruleId, dto));
	}

	@PostMapping("/create-availability-overrides")
	public void createTherapistAvailabilityOverrides(@RequestBody TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) throws JsonProcessingException {
		String therapistId = SecurityUtils.getTherapistId();
		therapistAvailabilityOverridesDto.setTherapistId(therapistId);

		therapistService.createTherapistAvailabilityOverrides(therapistAvailabilityOverridesDto);
	}

	@PostMapping("/bulk-availability-overrides")
	public ResponseEntity<String> createBulkAvailabilityOverrides(@RequestBody BulkAvailabilityOverridesRequest request) throws JsonProcessingException {
		String therapistId = SecurityUtils.getTherapistId();
		therapistService.createBulkAvailabilityOverrides(therapistId, request);
		return ResponseEntity.ok("Bulk availability overrides created successfully.");
	}

	@GetMapping("/availability-overrides")
	public ResponseEntity<List<TherapistAvailabilityOverridesDto>> getAvailabilityOverrides() {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getAllTherapistAvailabilityOverrides(therapistId));
	}

	@DeleteMapping("/availability-overrides/{overrideId}")
	public ResponseEntity<String> deleteAvailabilityOverride(@PathVariable String overrideId) throws JsonProcessingException {
		String therapistId = SecurityUtils.getTherapistId();
		therapistService.deleteTherapistAvailabilityOverride(therapistId, overrideId);
		return ResponseEntity.ok("Availability override deleted successfully.");
	}

	@PostMapping("/generate-slots")
	public ResponseEntity<String> generateSlots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		if (endDate.isBefore(startDate)) {
			return ResponseEntity.badRequest().body("End date cannot be before start date.");
		}

		String therapistId = SecurityUtils.getTherapistId();

		try {
			availabilitySlotService.generateAvailabilitySlots(therapistId, startDate, endDate);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		return ResponseEntity.ok(String.format("Successfully generated slots"));
	}

	@DeleteMapping("/delete-slots")
	public ResponseEntity<String> deleteSlots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){

		if (endDate.isBefore(startDate)) {
			return ResponseEntity.badRequest().body("End date cannot be before start date.");
		}

		String therapistId = SecurityUtils.getTherapistId();

		try {
			availabilitySlotService.deleteAvailabilitySlots(therapistId, startDate, endDate);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}

		return ResponseEntity.ok(String.format("Successfully deleted slots"));
	}

	@PostMapping("/{clientId}/create-notes")
	public void createNotes(@PathVariable String clientId, @RequestBody SessionNotesDto sessionNotesDto) {

		String therapistId = SecurityUtils.getTherapistId();
		sessionNotesDto.setTherapistId(therapistId);
		therapistService.createNotes(sessionNotesDto);
	}

	@PutMapping("/{clientId}/update-notes")
	public void updateNotes(@PathVariable String clientId, @RequestBody SessionNotesDto sessionNotesDto) {

		String therapistId = SecurityUtils.getTherapistId();
		sessionNotesDto.setTherapistId(therapistId);
		therapistService.updateNotes(sessionNotesDto);
	}

	@GetMapping("/{clientId}/session-details")
	public ResponseEntity<List<SessionDetailsDto>> getClientAppointmentHistory(@PathVariable String clientId){

		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getClientAppointmentHistory(therapistId, clientId));
	}

	@GetMapping("/{clientId}/note")
	public ResponseEntity<ClientNotesDto> getClientNotes(@PathVariable String clientId) {
		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(therapistService.getClientNote(therapistId, clientId));
	}

	@PutMapping("/{clientId}/note")
	public ResponseEntity<ClientNotesDto> putClientNote(@PathVariable String clientId, @RequestBody ClientNotesDto clientNotesDto) {
		String therapistId = SecurityUtils.getTherapistId();
		clientNotesDto.setTherapistId(therapistId);
		return ResponseEntity.ok(therapistService.putClientNote(therapistId, clientId, clientNotesDto));
	}

}
