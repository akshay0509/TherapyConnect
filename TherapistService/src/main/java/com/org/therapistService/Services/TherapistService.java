package com.org.therapistService.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.therapistService.Assembler.TherapistAssembler;
import com.org.therapistService.Dto.SessionDetailsDto;
import com.org.therapistService.Dto.SessionNotesDto;
import com.org.therapistService.Dto.TherapistAvailabilityDto;
import com.org.therapistService.Dto.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Dto.TherapistAvailabilityRulesDto;
import com.org.therapistService.Dto.TherapistClientsDto;
import com.org.therapistService.Dto.TherapistDto;
import com.org.therapistService.Dto.TherapistServicesDto;
import com.org.therapistService.Entity.AppointmentProjection;
import com.org.therapistService.Entity.SessionNotes;
import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistClients;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Repository.AppointmentProjectionRepository;
import com.org.therapistService.Repository.SessionNotesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistClientsRepository;
import com.org.therapistService.Repository.TherapistRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

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
	
	private static final Logger logger = LoggerFactory.getLogger(TherapistService.class);
	
	private TherapistAssembler therapistAssembler = new TherapistAssembler();
	
	public void createTherapist(TherapistDto therapistDto) {
		Therapist therapist = therapistAssembler.assembleDtoToEntity(therapistDto);
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
	
	public List<TherapistAvailabilityDto> getTherapistAvailability(String therapistId) {
		List<TherapistAvailability> list = therapistAvailabilityRepository.findByTherapistId(therapistId);
		List<TherapistAvailabilityDto> dtoList = new ArrayList<TherapistAvailabilityDto>();
		TherapistAvailabilityDto dto;
		for(TherapistAvailability rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
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
	
	public void createTherapistAvailabilityOverrides(TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) {
		TherapistAvailabilityOverrides therapistAvailabilityOverrides = therapistAssembler.assembleDtoToEntity(therapistAvailabilityOverridesDto);
		therapistAvailabilityOverridesRepository.save(therapistAvailabilityOverrides);
	}
	
	public List<TherapistAvailabilityOverridesDto> getAllTherapistAvailabilityOverrides(){
		List<TherapistAvailabilityOverrides> list = therapistAvailabilityOverridesRepository.findAll();
		List<TherapistAvailabilityOverridesDto> dtoList = new ArrayList<TherapistAvailabilityOverridesDto>();
		TherapistAvailabilityOverridesDto dto;
		for(TherapistAvailabilityOverrides rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
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
	
	public void addClient(String therapistId, String clientId, String clientName) {
		TherapistClients therapistClient = new TherapistClients();
		therapistClient.setClientId(clientId);
		therapistClient.setClientName(clientName);
		therapistClient.setTherapistId(therapistId);
		
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
	
}
