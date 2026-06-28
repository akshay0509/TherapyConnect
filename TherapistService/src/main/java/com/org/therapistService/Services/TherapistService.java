package com.org.therapistService.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.events.Client.ClientStatus;
import com.org.events.TherapistAppointment.AppointmentStatus;
import com.org.events.TherapistAvailability.AvailabilityOverrideEvent;
import com.org.events.TherapistAvailability.CalendarBlockEvent;
import com.org.events.TherapistAvailability.DeliveryModeEvent;
import com.org.therapistService.Assembler.TherapistAssembler;
import com.org.therapistService.Dto.BulkAvailabilityOverridesRequest;
import com.org.therapistService.Dto.ClientNotesDto;
import com.org.therapistService.Dto.DashboardStatsDto;
import com.org.therapistService.Dto.EarningsSessionDto;
import com.org.therapistService.Dto.EarningsSummaryDto;
import com.org.therapistService.Dto.PageResponseDto;
import com.org.therapistService.Dto.SessionDetailsDto;
import com.org.therapistService.Dto.SessionNotesDto;
import com.org.therapistService.Dto.SlotDeliveryOptionDto;
import com.org.therapistService.Dto.TherapistAvailabilityDto;
import com.org.therapistService.Dto.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Dto.TherapistAvailabilityRulesDto;
import com.org.therapistService.Dto.TherapistClientsDto;
import com.org.therapistService.Dto.TherapistDto;
import com.org.therapistService.Dto.TherapistServicesDto;
import com.org.therapistService.Dto.TherapyDeliveryModeDto;
import com.org.therapistService.Entity.AppointmentProjection;
import com.org.therapistService.Entity.ClientNotes;
import com.org.therapistService.Entity.SessionNotes;
import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistClients;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Entity.TherapyDeliveryMode;
import com.org.therapistService.Repository.AppointmentProjectionRepository;
import com.org.therapistService.Repository.ClientNotesRepository;
import com.org.therapistService.Repository.SessionNotesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistClientsRepository;
import com.org.therapistService.Repository.TherapistRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;
import com.org.therapistService.Repository.TherapyDeliveryModeRepository;

import jakarta.transaction.Transactional;

@Service
public class TherapistService {

	@Autowired
	private TherapistRepository therapistRepository;

	@Autowired
	private TherapistServicesRepository therapistServicesRepository;

	@Autowired
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;

	@Autowired
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Autowired
	private TherapistClientsRepository therapistClientsRepository;

	@Autowired
	private SessionNotesRepository sessionNotesRepository;

	@Autowired
	private AppointmentProjectionRepository appointmentProjectionRepository;

	@Autowired
	private ClientNotesRepository clientNotesRepository;

	@Autowired
	private OutboxService outboxService;

	@Autowired
	private AvailabilitySlotService availabilitySlotService;

	@Autowired
	private TherapyDeliveryModeRepository therapyDeliveryModeRepository;

	private static final Logger logger = LoggerFactory.getLogger(TherapistService.class);

	private TherapistAssembler therapistAssembler = new TherapistAssembler();

	public void createTherapist(TherapistDto therapistDto, String userId) {
		Therapist therapist = therapistAssembler.assembleDtoToEntity(therapistDto);
		therapist.setUserId(userId);
		therapistRepository.save(therapist);
	}

	public List<TherapistDto> getAllTherapists(){
		List<Therapist> list = therapistRepository.findAll();
		List<TherapistDto> dtoList = new ArrayList<TherapistDto>();
		TherapistDto dto;
		for(Therapist rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public TherapistDto getTherapist(String therapistId){
		Therapist therapist = therapistRepository.findByTherapistId(therapistId);
		TherapistDto dto = therapistAssembler.assembleEntityToDto(therapist);
		return dto;
	}

	public void createTherapistServices(TherapistServicesDto therapistServicesDto) {
		TherapistServices therapistServices = therapistAssembler.assembleDtoToEntity(therapistServicesDto);
		therapistServicesRepository.save(therapistServices);
	}

	@Transactional
	public TherapistServicesDto updateTherapistService(String therapistId, String serviceId, TherapistServicesDto therapistServicesDto) {
		TherapistServices service = therapistServicesRepository.findByServiceIdAndTherapistId(serviceId, therapistId)
				.orElseThrow(() -> new IllegalArgumentException("Therapist service not found."));

		service.setServiceType(therapistServicesDto.getServiceType());
		service.setDuration(therapistServicesDto.getDuration());
		service.setPrice(therapistServicesDto.getPrice());
		service.setActive(Boolean.TRUE.equals(therapistServicesDto.getIsActive()));

		return therapistAssembler.assembleEntityToDto(therapistServicesRepository.save(service));
	}

	@Transactional
	public void deleteTherapistService(String therapistId, String serviceId) {
		TherapistServices service = therapistServicesRepository.findByServiceIdAndTherapistId(serviceId, therapistId)
				.orElseThrow(() -> new IllegalArgumentException("Therapist service not found."));
		therapistServicesRepository.delete(service);
	}

	public List<TherapistServicesDto> getAllTherapistServices(){
		List<TherapistServices> list = therapistServicesRepository.findAll();
		List<TherapistServicesDto> dtoList = new ArrayList<TherapistServicesDto>();
		TherapistServicesDto dto;
		for(TherapistServices rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public List<TherapistServicesDto> getTherapistServices(String therapistId){
		List<TherapistServices> list = therapistServicesRepository.findByTherapistId(therapistId);
		List<TherapistServicesDto> dtoList = new ArrayList<TherapistServicesDto>();
		TherapistServicesDto dto;
		for(TherapistServices rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public PageResponseDto<TherapistServicesDto> getTherapistServices(String therapistId, int page, int size) {
		Page<TherapistServices> result = therapistServicesRepository.findByTherapistId(therapistId, PageRequest.of(page, size));
		return new PageResponseDto<>(
				result.getContent().stream().map(therapistAssembler::assembleEntityToDto).toList(),
				result.getTotalPages(),
				result.getTotalElements(),
				result.getNumber(),
				result.getSize());
	}

	public List<TherapistAvailabilityDto> getTherapistAvailability(String therapistId) {
		List<TherapistAvailability> slots = therapistAvailabilityRepository.findByTherapistId(therapistId);

		List<TherapyDeliveryMode> allModes = therapyDeliveryModeRepository.findByTherapistIdAndIsActiveTrue(therapistId);

		Map<String, List<TherapyDeliveryMode>> modesByServiceId = new HashMap<String, List<TherapyDeliveryMode>>();
		for(TherapyDeliveryMode mode : allModes) {
			if(!modesByServiceId.containsKey(mode.getServiceId())) {
				modesByServiceId.put(mode.getServiceId(), new ArrayList<TherapyDeliveryMode>());
			}
			modesByServiceId.get(mode.getServiceId()).add(mode);
		}

		List<TherapistAvailabilityDto> dtoList = new ArrayList<TherapistAvailabilityDto>();
		for(TherapistAvailability slot : slots) {
			TherapistAvailabilityDto dto = therapistAssembler.assembleEntityToDto(slot);
			List<TherapyDeliveryMode> modesForService = modesByServiceId.getOrDefault(slot.getServiceId(), new ArrayList<TherapyDeliveryMode>());
			List<SlotDeliveryOptionDto> optionDtos = new ArrayList<SlotDeliveryOptionDto>();
			for(TherapyDeliveryMode mode : modesForService) {
				SlotDeliveryOptionDto optionDto = therapistAssembler.assembleModeToOptionDto(mode);
				optionDtos.add(optionDto);
			}
			dto.setDeliveryOptions(optionDtos);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public List<TherapistAvailabilityDto> getAllTherapistAvailability(){
		List<TherapistAvailability> list = therapistAvailabilityRepository.findAll();
		List<TherapistAvailabilityDto> dtoList = new ArrayList<TherapistAvailabilityDto>();
		TherapistAvailabilityDto dto;
		for(TherapistAvailability rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	@Transactional
	public TherapyDeliveryModeDto createDeliveryMode(String therapistId, TherapyDeliveryModeDto dto) throws JsonProcessingException {
		TherapyDeliveryMode mode = therapistAssembler.assembleDtoToEntity(dto);
		mode.setTherapistId(therapistId);
		TherapyDeliveryMode saved = therapyDeliveryModeRepository.save(mode);

		DeliveryModeEvent event = buildDeliveryModeEvent("DeliveryModeCreated", saved);
		outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", therapistId, "DeliveryModeCreated", event);

		return therapistAssembler.assembleEntityToDto(saved);
	}

	public List<TherapyDeliveryModeDto> getDeliveryModes(String therapistId) {
		List<TherapyDeliveryMode> list = therapyDeliveryModeRepository.findByTherapistId(therapistId);
		List<TherapyDeliveryModeDto> dtoList = new ArrayList<TherapyDeliveryModeDto>();
		TherapyDeliveryModeDto dto;
		for(TherapyDeliveryMode rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public List<TherapyDeliveryModeDto> getDeliveryModesForService(String therapistId, String serviceId) {
		List<TherapyDeliveryMode> list = therapyDeliveryModeRepository.findByTherapistIdAndServiceIdAndIsActiveTrue(therapistId, serviceId);
		List<TherapyDeliveryModeDto> dtoList = new ArrayList<TherapyDeliveryModeDto>();
		TherapyDeliveryModeDto dto;
		for(TherapyDeliveryMode rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	@Transactional
	public TherapyDeliveryModeDto updateDeliveryMode(String therapistId, String modeId, TherapyDeliveryModeDto dto) throws JsonProcessingException {
		TherapyDeliveryMode mode = therapyDeliveryModeRepository.findByModeIdAndTherapistId(modeId, therapistId)
				.orElseThrow(() -> new IllegalArgumentException("Delivery mode not found."));
		mode.setModeType(dto.getModeType());
		mode.setDisplayName(dto.getDisplayName());
		mode.setAddress(dto.getAddress());
		mode.setPrice(dto.getPrice());
		mode.setActive(Boolean.TRUE.equals(dto.getIsActive()));
		TherapyDeliveryMode saved = therapyDeliveryModeRepository.save(mode);

		DeliveryModeEvent event = buildDeliveryModeEvent("DeliveryModeUpdated", saved);
		outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", therapistId, "DeliveryModeUpdated", event);

		return therapistAssembler.assembleEntityToDto(saved);
	}

	@Transactional
	public void deleteDeliveryMode(String therapistId, String modeId) throws JsonProcessingException {
		TherapyDeliveryMode mode = therapyDeliveryModeRepository.findByModeIdAndTherapistId(modeId, therapistId)
				.orElseThrow(() -> new IllegalArgumentException("Delivery mode not found."));
		therapyDeliveryModeRepository.delete(mode);

		DeliveryModeEvent event = new DeliveryModeEvent();
		event.setEventType("DeliveryModeDeleted");
		event.setModeId(modeId);
		event.setTherapistId(therapistId);
		outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", therapistId, "DeliveryModeDeleted", event);
	}

	public void createTherapistAvailabilityRules(List<TherapistAvailabilityRulesDto> therapistAvailabilityRulesDtoList) {
		if(!therapistAvailabilityRulesDtoList.isEmpty()) {
			List<TherapistAvailabilityRules> therapistAvailabilityRulesList = new ArrayList<TherapistAvailabilityRules>();
			for(TherapistAvailabilityRulesDto therapistAvailabilityRulesDto : therapistAvailabilityRulesDtoList) {
				TherapistAvailabilityRules therapistAvailabilityRules = therapistAssembler.assembleDtoToEntity(therapistAvailabilityRulesDto);
				therapistAvailabilityRulesList.add(therapistAvailabilityRules);
			}
			therapistAvailabilityRulesRepository.saveAll(therapistAvailabilityRulesList);
		}
	}

	@Transactional
	public void deleteAvailabilityRule(String therapistId, String ruleId) {
		TherapistAvailabilityRules rule = therapistAvailabilityRulesRepository.findByRuleIdAndTherapistId(ruleId, therapistId)
				.orElseThrow(() -> new IllegalArgumentException("Availability rule not found."));
		therapistAvailabilityRulesRepository.delete(rule);
	}

	@Transactional
	public TherapistAvailabilityRulesDto updateAvailabilityRule(String therapistId, String ruleId, TherapistAvailabilityRulesDto dto) {
		TherapistAvailabilityRules rule = therapistAvailabilityRulesRepository.findByRuleIdAndTherapistId(ruleId, therapistId)
				.orElseThrow(() -> new IllegalArgumentException("Availability rule not found."));
		rule.setDayOfWeek(dto.getDayOfWeek());
		rule.setStartTime(dto.getStartTime());
		rule.setEndTime(dto.getEndTime());
		rule.setActive(Boolean.TRUE.equals(dto.getIsActive()));
		return therapistAssembler.assembleEntityToDto(therapistAvailabilityRulesRepository.save(rule));
	}

	public List<TherapistAvailabilityRulesDto> getAllTherapistAvailabilityRules(String therapistId){
		List<TherapistAvailabilityRules> list = therapistAvailabilityRulesRepository.findByTherapistId(therapistId);
		List<TherapistAvailabilityRulesDto> dtoList = new ArrayList<TherapistAvailabilityRulesDto>();
		TherapistAvailabilityRulesDto dto;
		for(TherapistAvailabilityRules rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public PageResponseDto<TherapistAvailabilityRulesDto> getAllTherapistAvailabilityRules(String therapistId, int page, int size) {
		Page<TherapistAvailabilityRules> result = therapistAvailabilityRulesRepository.findByTherapistId(therapistId, PageRequest.of(page, size));
		return new PageResponseDto<>(
				result.getContent().stream().map(therapistAssembler::assembleEntityToDto).toList(),
				result.getTotalPages(),
				result.getTotalElements(),
				result.getNumber(),
				result.getSize());
	}

	@Transactional
	public void createTherapistAvailabilityOverrides(TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) throws JsonProcessingException {

		validateAvailabilityOverride(therapistAvailabilityOverridesDto);

		TherapistAvailabilityOverrides therapistAvailabilityOverrides = therapistAssembler.assembleDtoToEntity(therapistAvailabilityOverridesDto);
		TherapistAvailabilityOverrides saved = therapistAvailabilityOverridesRepository.save(therapistAvailabilityOverrides);

		AvailabilityOverrideEvent overrideEvent = new AvailabilityOverrideEvent();
		overrideEvent.setEventType("AvailabilityOverrideCreated");
		overrideEvent.setOverrideId(saved.getOverrideId());
		overrideEvent.setTherapistId(saved.getTherapistId());
		overrideEvent.setStartTime(saved.getStartTime());
		overrideEvent.setEndTime(saved.getEndTime());
		overrideEvent.setAvailable(saved.isAvailable());
		overrideEvent.setReason(saved.getReason());

		outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", saved.getTherapistId(), "AvailabilityOverrideCreated", overrideEvent);

		if (saved.isAvailable()) {
			availabilitySlotService.applyCreatedOverride(saved.getTherapistId(), saved);
		}

		if (!saved.isAvailable() && Boolean.TRUE.equals(saved.getSyncToGoogleCalendar())) {
			CalendarBlockEvent calendarBlockEvent = new CalendarBlockEvent();
			calendarBlockEvent.setEventType("CalendarBlockCreated");
			calendarBlockEvent.setBlockId(saved.getOverrideId());
			calendarBlockEvent.setTherapistId(saved.getTherapistId());
			calendarBlockEvent.setStartTime(saved.getStartTime());
			calendarBlockEvent.setEndTime(saved.getEndTime());
			calendarBlockEvent.setReason(saved.getReason());
			calendarBlockEvent.setSyncToGoogleCalendar(true);

			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", saved.getTherapistId(), "CalendarBlockCreated", calendarBlockEvent);
		}
	}

	@Transactional
	public void createBulkAvailabilityOverrides(String therapistId, BulkAvailabilityOverridesRequest request) throws JsonProcessingException {
		if (request.getStartDate() == null || request.getEndDate() == null) {
			throw new IllegalArgumentException("Start date and end date are required.");
		}
		if (request.getEndDate().isBefore(request.getStartDate())) {
			throw new IllegalArgumentException("End date cannot be before start date.");
		}

		for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
			TherapistAvailabilityOverridesDto dto = new TherapistAvailabilityOverridesDto();
			dto.setTherapistId(therapistId);
			dto.setStartTime(LocalDateTime.of(date, LocalTime.MIN));
			dto.setEndTime(LocalDateTime.of(date, LocalTime.MAX));
			dto.setIsAvailable(Boolean.TRUE.equals(request.getIsAvailable()));
			dto.setReason(request.getReason());
			dto.setSyncToGoogleCalendar(Boolean.TRUE.equals(request.getSyncToGoogleCalendar()));
			createTherapistAvailabilityOverrides(dto);
		}
	}

	public List<TherapistAvailabilityOverridesDto> getAllTherapistAvailabilityOverrides(String therapistId){
		List<TherapistAvailabilityOverrides> list = therapistAvailabilityOverridesRepository.findByTherapistIdOrderByStartTimeAsc(therapistId);
		List<TherapistAvailabilityOverridesDto> dtoList = new ArrayList<TherapistAvailabilityOverridesDto>();
		TherapistAvailabilityOverridesDto dto;
		for(TherapistAvailabilityOverrides rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	@Transactional
	public void deleteTherapistAvailabilityOverride(String therapistId, String overrideId) throws JsonProcessingException {

		TherapistAvailabilityOverrides override = therapistAvailabilityOverridesRepository.findById(overrideId)
				.orElseThrow(() -> new IllegalArgumentException("Availability override not found."));

		if (!override.getTherapistId().equals(therapistId)) {
			throw new IllegalArgumentException("Availability override does not belong to therapist.");
		}

		if (override.isAvailable()) {
			availabilitySlotService.applyDeletedOverride(therapistId, override);
		}

		AvailabilityOverrideEvent overrideEvent = new AvailabilityOverrideEvent();
		overrideEvent.setEventType("AvailabilityOverrideDeleted");
		overrideEvent.setOverrideId(override.getOverrideId());
		overrideEvent.setTherapistId(override.getTherapistId());
		overrideEvent.setStartTime(override.getStartTime());
		overrideEvent.setEndTime(override.getEndTime());
		overrideEvent.setAvailable(override.isAvailable());
		overrideEvent.setReason(override.getReason());
		outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", override.getTherapistId(), "AvailabilityOverrideDeleted", overrideEvent);

		if (!override.isAvailable() && Boolean.TRUE.equals(override.getSyncToGoogleCalendar())) {
			CalendarBlockEvent calendarBlockEvent = new CalendarBlockEvent();
			calendarBlockEvent.setEventType("CalendarBlockDeleted");
			calendarBlockEvent.setBlockId(override.getOverrideId());
			calendarBlockEvent.setTherapistId(override.getTherapistId());
			calendarBlockEvent.setOccurredAt(LocalDateTime.now());

			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", override.getTherapistId(), "CalendarBlockDeleted", calendarBlockEvent);
		}

		therapistAvailabilityOverridesRepository.delete(override);

	}

	public String getTherapistIdByUserId(String userId) {
		Therapist therapist = therapistRepository.findByUserId(userId);
		return therapist.getTherapistId();
	}

	public List<TherapistClientsDto> getClientsForTherapist(String therapistId){
		List<TherapistClients> list = therapistClientsRepository.findByTherapistId(therapistId);
		List<TherapistClientsDto> dtoList = new ArrayList<TherapistClientsDto>();
		TherapistClientsDto dto;
		for(TherapistClients rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}

	public PageResponseDto<TherapistClientsDto> getClientsForTherapist(String therapistId, int page, int size) {
		Page<TherapistClients> result = therapistClientsRepository.findByTherapistId(therapistId, PageRequest.of(page, size));
		return new PageResponseDto<>(
				result.getContent().stream().map(therapistAssembler::assembleEntityToDto).toList(),
				result.getTotalPages(),
				result.getTotalElements(),
				result.getNumber(),
				result.getSize());
	}

	public void addClient(String therapistId, String clientId, String clientName, boolean dsf) {
		TherapistClients therapistClient = new TherapistClients();
		therapistClient.setClientId(clientId);
		therapistClient.setClientName(clientName);
		therapistClient.setTherapistId(therapistId);
		therapistClient.setDsf(dsf);

		therapistClientsRepository.save(therapistClient);
	}

	public List<SessionDetailsDto> getClientAppointmentHistory(String therapistId, String clientId) {
		return sessionNotesRepository.findAppointmentsWithNotes(therapistId, clientId);
	}

	public void createNotes(SessionNotesDto sessionNotesDto) {
		logger.info("inside createNotes..");
		SessionNotes sessionNotes = therapistAssembler.assembleDtoToEntity(sessionNotesDto);
		logger.info("therapist id :"+sessionNotes.getTherapistId());
		logger.info("appointment id :"+sessionNotes.getAppointmentId());
		AppointmentProjection appointmentProjection = appointmentProjectionRepository.findByAppointmentIdAndTherapistId(sessionNotes.getAppointmentId(), sessionNotes.getTherapistId());
		logger.info("clientId id :"+appointmentProjection.getClientId());
		logger.info("notes :"+sessionNotes.getNoteContent());
		sessionNotes.setClientId(appointmentProjection.getClientId());
		sessionNotesRepository.save(sessionNotes);
		logger.info("exiting createNotes..");
	}

	public void updateNotes(SessionNotesDto sessionNotesDto) {

		String appointmentId = sessionNotesDto.getAppointmentId();
		SessionNotes sessionNotes = sessionNotesRepository.findByAppointmentId(appointmentId);
		sessionNotes.setNoteContent(sessionNotesDto.getSessionNotes());
		sessionNotes.setUpdatedAt(LocalDateTime.now());

		sessionNotesRepository.save(sessionNotes);

	}

	public DashboardStatsDto getDashboardStats(String therapistId) {

		LocalDate today = LocalDate.now();
		LocalDateTime startOfToday = today.atStartOfDay();
		LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

		LocalDate startOfWeekDate = today.minusDays(today.getDayOfWeek().getValue() - 1L);
		LocalDateTime startOfWeek = startOfWeekDate.atStartOfDay();
		LocalDateTime endOfWeek = startOfWeek.plusDays(7);

		LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
		LocalDateTime lifetimeStart = LocalDate.of(1970, 1, 1).atStartOfDay();

		long sessionsToday = appointmentProjectionRepository.countByTherapistIdAndStatusInAndStartTimeBetween(
				therapistId,
				List.of(
						AppointmentStatus.CONFIRMED,
						AppointmentStatus.RESCHEDULED,
						AppointmentStatus.COMPLETED
						),
				startOfToday,
				endOfToday);

		long activeClients = therapistClientsRepository.countByTherapistIdAndStatus(therapistId, ClientStatus.ACTIVE);

		long completedThisWeek = appointmentProjectionRepository.countByTherapistIdAndStatusAndStartTimeBetween(
				therapistId,
				AppointmentStatus.COMPLETED,
				startOfWeek,
				endOfWeek
				);

		return new DashboardStatsDto(
				sessionsToday,
				activeClients,
				completedThisWeek,
				calculatePaidEarnings(therapistId, startOfToday, endOfToday),
				calculatePaidEarnings(therapistId, startOfWeek, endOfWeek),
				calculatePaidEarnings(therapistId, startOfMonth, endOfMonth),
				calculatePaidEarnings(therapistId, lifetimeStart, endOfToday)
				);
	}

	private BigDecimal calculatePaidEarnings(String therapistId, LocalDateTime start, LocalDateTime end) {
		return toMoney(appointmentProjectionRepository.sumPaidCompletedEarningsBetween(therapistId, start, end));
	}

	private BigDecimal toMoney(BigDecimal amount) {
		return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
	}

	public EarningsSummaryDto getEarningsSummary(String therapistId) {
		LocalDate today = LocalDate.now();
		LocalDateTime now = today.plusDays(1).atStartOfDay();

		LocalDateTime weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime lifetimeStart = LocalDate.of(1970, 1, 1).atStartOfDay();

		BigDecimal weekEarnings   = toMoney(appointmentProjectionRepository.sumPaidCompletedEarningsBetween(therapistId, weekStart, now));
		BigDecimal monthEarnings  = toMoney(appointmentProjectionRepository.sumPaidCompletedEarningsBetween(therapistId, monthStart, now));
		BigDecimal lifetimeEarnings = toMoney(appointmentProjectionRepository.sumPaidCompletedEarningsBetween(therapistId, lifetimeStart, now));

		long weekPaidCount      = appointmentProjectionRepository.countPaidCompletedBetween(therapistId, weekStart, now);
		long monthPaidCount     = appointmentProjectionRepository.countPaidCompletedBetween(therapistId, monthStart, now);
		long lifetimePaidCount  = appointmentProjectionRepository.countPaidCompletedBetween(therapistId, lifetimeStart, now);

		long weekDsfCount      = appointmentProjectionRepository.countDsfCompletedBetween(therapistId, weekStart, now);
		long monthDsfCount     = appointmentProjectionRepository.countDsfCompletedBetween(therapistId, monthStart, now);
		long lifetimeDsfCount  = appointmentProjectionRepository.countDsfCompletedBetween(therapistId, lifetimeStart, now);

		return new EarningsSummaryDto(
				weekEarnings, monthEarnings, lifetimeEarnings,
				weekPaidCount, monthPaidCount, lifetimePaidCount,
				weekDsfCount, monthDsfCount, lifetimeDsfCount
				);
	}

	public List<EarningsSessionDto> getEarningsSessions(
			String therapistId,
			LocalDate fromDate,
			LocalDate toDate,
			String serviceId,
			String modeId) {
		LocalDateTime from = fromDate.atStartOfDay();
		LocalDateTime to = toDate.plusDays(1).atStartOfDay();
		return appointmentProjectionRepository.findEarningsSessions(
				therapistId, from, to,
				(serviceId != null && !serviceId.isBlank()) ? serviceId : null,
				(modeId != null && !modeId.isBlank()) ? modeId : null
				);
	}

	public byte[] exportEarningsCsv(
			String therapistId,
			LocalDate fromDate,
			LocalDate toDate,
			String serviceId,
			String modeId) {
		List<EarningsSessionDto> sessions = getEarningsSessions(therapistId, fromDate, toDate, serviceId, modeId);
		StringBuilder csv = new StringBuilder();
		csv.append("appointmentId,clientId,clientName,serviceId,modeId,startTime,endTime,sessionFee,dsf,earningAmount\n");
		for (EarningsSessionDto session : sessions) {
			csv.append(csv(session.getAppointmentId())).append(',')
					.append(csv(session.getClientId())).append(',')
					.append(csv(session.getClientName())).append(',')
					.append(csv(session.getServiceId())).append(',')
					.append(csv(session.getModeId())).append(',')
					.append(csv(session.getStartTime())).append(',')
					.append(csv(session.getEndTime())).append(',')
					.append(csv(toMoney(session.getSessionFee()))).append(',')
					.append(session.isDsf()).append(',')
					.append(csv(toMoney(session.getEarningAmount()))).append('\n');
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String csv(Object value) {
		if (value == null) return "";
		String text = String.valueOf(value);
		if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
			return "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}

	private DeliveryModeEvent buildDeliveryModeEvent(String eventType, TherapyDeliveryMode mode) {
		DeliveryModeEvent event = new DeliveryModeEvent();
		event.setEventType(eventType);
		event.setModeId(mode.getModeId());
		event.setTherapistId(mode.getTherapistId());
		event.setServiceId(mode.getServiceId());
		event.setModeType(mode.getModeType().name());
		event.setDisplayName(mode.getDisplayName());
		event.setAddress(mode.getAddress());
		event.setPrice(mode.getPrice());
		event.setIsActive(mode.isActive());
		return event;
	}

	private void validateAvailabilityOverride(TherapistAvailabilityOverridesDto dto) {

		if (dto.getStartTime() == null || dto.getEndTime() == null) {
			throw new IllegalArgumentException("Start time and end time are required.");
		}

		if (!dto.getEndTime().isAfter(dto.getStartTime())) {
			throw new IllegalArgumentException("End time must be after start time.");
		}

		if (!dto.getStartTime().toLocalDate().equals(dto.getEndTime().toLocalDate())) {
			throw new IllegalArgumentException("Availability overrides currently support same-day windows only.");
		}

		if (Boolean.TRUE.equals(dto.getIsAvailable()) && overlapsBaseAvailability(dto)) {
			throw new IllegalArgumentException(
					"Available overrides must be outside the therapist's normal availability window.");
		}
	}

	private boolean overlapsBaseAvailability(TherapistAvailabilityOverridesDto dto) {
		int dayOfWeek = dto.getStartTime().getDayOfWeek().getValue();

		return therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(dto.getTherapistId()).stream()
				.filter(rule -> rule.getDayOfWeek() == dayOfWeek)
				.anyMatch(rule -> overlaps(
						dto.getStartTime().toLocalTime(),
						dto.getEndTime().toLocalTime(),
						rule.getStartTime(),
						rule.getEndTime()));
	}

	private boolean overlaps(LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
		return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
	}

	public ClientNotesDto getClientNote(String therapistId, String clientId) {

		ClientNotes clientNote = clientNotesRepository.findByTherapistIdAndClientId(therapistId, clientId);

		if (clientNote == null) {
			ClientNotesDto clientNotesDto = new ClientNotesDto();
			clientNotesDto.setTherapistId(therapistId);
			clientNotesDto.setClientId(clientId);
			clientNotesDto.setContent("");
			return clientNotesDto;
		}

		ClientNotesDto dto = therapistAssembler.assembleEntityToDto(clientNote);
		return dto;

	}

	@Transactional
	public ClientNotesDto putClientNote(String therapistId, String clientId, ClientNotesDto clientNotesDto) {

		ClientNotes clientNote = clientNotesRepository.findByTherapistIdAndClientId(therapistId, clientId);

		if(clientNote == null) {
			clientNote = therapistAssembler.assembleDtoToEntity(clientNotesDto);
			clientNote.setClientId(clientId);
		}

		else {
			clientNote.setContent(clientNotesDto.getContent());
			clientNote.setUpdatedAt(LocalDateTime.now());
		}

		clientNotesRepository.save(clientNote);
		clientNotesDto = therapistAssembler.assembleEntityToDto(clientNote);
		return clientNotesDto;
	}

}
