package com.org.therapistService.Assembler;

import java.time.LocalDateTime;

import com.org.therapistService.Dto.ClientNotesDto;
import com.org.therapistService.Dto.SessionNotesDto;
import com.org.therapistService.Dto.SlotDeliveryOptionDto;
import com.org.therapistService.Dto.TherapistAvailabilityDto;
import com.org.therapistService.Dto.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Dto.TherapistAvailabilityRulesDto;
import com.org.therapistService.Dto.TherapistClientsDto;
import com.org.therapistService.Dto.TherapistDto;
import com.org.therapistService.Dto.TherapistServicesDto;
import com.org.therapistService.Dto.TherapyDeliveryModeDto;
import com.org.therapistService.Entity.ClientNotes;
import com.org.therapistService.Entity.SessionNotes;
import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistClients;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Entity.TherapyDeliveryMode;

public class TherapistAssembler {

	public Therapist assembleDtoToEntity(TherapistDto therapistDto) {
		Therapist therapist = new Therapist();
		therapist.setFirstName(therapistDto.getFirstName());
		therapist.setLastName(therapistDto.getLastName());
		therapist.setDob(therapistDto.getDob());
		therapist.setEmail(therapistDto.getEmail());
		therapist.setGender(therapistDto.getGender());
		therapist.setPhoneNumber(therapistDto.getPhoneNumber());
		therapist.setYearsOfExperience(therapistDto.getYearsOfExperience());
		therapist.setTimezone(therapistDto.getTimezone());

		return therapist;
	}

	public TherapistDto assembleEntityToDto(Therapist therapist) {
		TherapistDto therapistDto = new TherapistDto();
		therapistDto.setTherapistId(therapist.getTherapistId());
		therapistDto.setFirstName(therapist.getFirstName());
		therapistDto.setLastName(therapist.getLastName());
		therapistDto.setDob(therapist.getDob());
		therapistDto.setEmail(therapist.getEmail());
		therapistDto.setGender(therapist.getGender());
		therapistDto.setPhoneNumber(therapist.getPhoneNumber());
		therapistDto.setYearsOfExperience(therapist.getYearsOfExperience());
		therapistDto.setTimezone(therapist.getTimezone());

		return therapistDto;
	}

	public TherapistServices assembleDtoToEntity(TherapistServicesDto therapistServicesDto) {
		TherapistServices therapistServices = new TherapistServices();
		therapistServices.setTherapistId(therapistServicesDto.getTherapistId());
		therapistServices.setServiceType(therapistServicesDto.getServiceType());
		therapistServices.setPrice(therapistServicesDto.getPrice());
		therapistServices.setDuration(therapistServicesDto.getDuration());
		therapistServices.setActive(therapistServicesDto.getIsActive());

		return therapistServices;
	}

	public TherapistServicesDto assembleEntityToDto(TherapistServices therapistServices) {
		TherapistServicesDto therapistServicesDto = new TherapistServicesDto();
		therapistServicesDto.setServiceId(therapistServices.getServiceId());
		therapistServicesDto.setTherapistId(therapistServices.getTherapistId());
		therapistServicesDto.setServiceType(therapistServices.getServiceType());
		therapistServicesDto.setPrice(therapistServices.getPrice());
		therapistServicesDto.setDuration(therapistServices.getDuration());
		therapistServicesDto.setIsActive(therapistServices.isActive());

		return therapistServicesDto;
	}

	public TherapistAvailabilityRules assembleDtoToEntity(TherapistAvailabilityRulesDto therapistAvailabilityRulesDto) {
		TherapistAvailabilityRules therapistAvailabilityRules = new TherapistAvailabilityRules();
		therapistAvailabilityRules.setTherapistId(therapistAvailabilityRulesDto.getTherapistId());
		therapistAvailabilityRules.setActive(therapistAvailabilityRulesDto.getIsActive());
		therapistAvailabilityRules.setDayOfWeek(therapistAvailabilityRulesDto.getDayOfWeek());
		therapistAvailabilityRules.setStartTime(therapistAvailabilityRulesDto.getStartTime());
		therapistAvailabilityRules.setEndTime(therapistAvailabilityRulesDto.getEndTime());

		return therapistAvailabilityRules;
	}

	public TherapistAvailabilityRulesDto assembleEntityToDto(TherapistAvailabilityRules therapistAvailabilityRules) {
		TherapistAvailabilityRulesDto therapistAvailabilityRulesDto = new TherapistAvailabilityRulesDto();
		therapistAvailabilityRulesDto.setRuleId(therapistAvailabilityRules.getRuleId());
		therapistAvailabilityRulesDto.setTherapistId(therapistAvailabilityRules.getTherapistId());
		therapistAvailabilityRulesDto.setIsActive(therapistAvailabilityRules.isActive());
		therapistAvailabilityRulesDto.setDayOfWeek(therapistAvailabilityRules.getDayOfWeek());
		therapistAvailabilityRulesDto.setStartTime(therapistAvailabilityRules.getStartTime());
		therapistAvailabilityRulesDto.setEndTime(therapistAvailabilityRules.getEndTime());

		return therapistAvailabilityRulesDto;
	}

	public TherapistAvailabilityOverrides assembleDtoToEntity(TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) {
		TherapistAvailabilityOverrides therapistAvailabilityOverrides = new TherapistAvailabilityOverrides();
		therapistAvailabilityOverrides.setTherapistId(therapistAvailabilityOverridesDto.getTherapistId());
		therapistAvailabilityOverrides.setStartTime(therapistAvailabilityOverridesDto.getStartTime());
		therapistAvailabilityOverrides.setEndTime(therapistAvailabilityOverridesDto.getEndTime());
		therapistAvailabilityOverrides.setAvailable(therapistAvailabilityOverridesDto.getIsAvailable());
		therapistAvailabilityOverrides.setReason(therapistAvailabilityOverridesDto.getReason());
		therapistAvailabilityOverrides.setSyncToGoogleCalendar(Boolean.TRUE.equals(therapistAvailabilityOverridesDto.getSyncToGoogleCalendar()));

		return therapistAvailabilityOverrides;
	}

	public TherapistAvailabilityOverridesDto assembleEntityToDto(TherapistAvailabilityOverrides therapistAvailabilityOverrides) {
		TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto = new TherapistAvailabilityOverridesDto();
		therapistAvailabilityOverridesDto.setOverrideId(therapistAvailabilityOverrides.getOverrideId());
		therapistAvailabilityOverridesDto.setTherapistId(therapistAvailabilityOverrides.getTherapistId());
		therapistAvailabilityOverridesDto.setStartTime(therapistAvailabilityOverrides.getStartTime());
		therapistAvailabilityOverridesDto.setEndTime(therapistAvailabilityOverrides.getEndTime());
		therapistAvailabilityOverridesDto.setIsAvailable(therapistAvailabilityOverrides.isAvailable());
		therapistAvailabilityOverridesDto.setReason(therapistAvailabilityOverrides.getReason());
		therapistAvailabilityOverridesDto.setSyncToGoogleCalendar(therapistAvailabilityOverrides.getSyncToGoogleCalendar());

		return therapistAvailabilityOverridesDto;
	}

	public TherapistAvailability assembleDtoToEntity(TherapistAvailabilityDto therapistAvailabilityDto) {
		TherapistAvailability therapistAvailability = new TherapistAvailability();
		therapistAvailability.setTherapistId(therapistAvailabilityDto.getTherapistId());
		therapistAvailability.setStartTime(therapistAvailabilityDto.getStartTime());
		therapistAvailability.setEndTime(therapistAvailabilityDto.getEndTime());
		therapistAvailability.setServiceId(therapistAvailabilityDto.getServiceId());

		return therapistAvailability;
	}

	public TherapistAvailabilityDto assembleEntityToDto(TherapistAvailability therapistAvailability) {
		TherapistAvailabilityDto therapistAvailabilityDto = new TherapistAvailabilityDto();
		therapistAvailabilityDto.setSlotId(therapistAvailability.getSlotId());
		therapistAvailabilityDto.setTherapistId(therapistAvailability.getTherapistId());
		therapistAvailabilityDto.setStartTime(therapistAvailability.getStartTime());
		therapistAvailabilityDto.setEndTime(therapistAvailability.getEndTime());
		therapistAvailabilityDto.setServiceId(therapistAvailability.getServiceId());
		// deliveryOptions populated by TherapistService.getTherapistAvailability
		return therapistAvailabilityDto;
	}

	public SlotDeliveryOptionDto assembleModeToOptionDto(TherapyDeliveryMode mode) {
		SlotDeliveryOptionDto dto = new SlotDeliveryOptionDto();
		dto.setModeId(mode.getModeId());
		dto.setDisplayName(mode.getDisplayName());
		dto.setModeType(mode.getModeType());
		dto.setAddress(mode.getAddress());
		dto.setPrice(mode.getPrice());
		return dto;
	}

	public TherapyDeliveryMode assembleDtoToEntity(TherapyDeliveryModeDto dto) {
		TherapyDeliveryMode mode = new TherapyDeliveryMode();
		mode.setTherapistId(dto.getTherapistId());
		mode.setServiceId(dto.getServiceId());
		mode.setModeType(dto.getModeType());
		mode.setDisplayName(dto.getDisplayName());
		mode.setAddress(dto.getAddress());
		mode.setPrice(dto.getPrice());
		mode.setActive(Boolean.TRUE.equals(dto.getIsActive()));
		return mode;
	}

	public TherapyDeliveryModeDto assembleEntityToDto(TherapyDeliveryMode mode) {
		TherapyDeliveryModeDto dto = new TherapyDeliveryModeDto();
		dto.setModeId(mode.getModeId());
		dto.setTherapistId(mode.getTherapistId());
		dto.setServiceId(mode.getServiceId());
		dto.setModeType(mode.getModeType());
		dto.setDisplayName(mode.getDisplayName());
		dto.setAddress(mode.getAddress());
		dto.setPrice(mode.getPrice());
		dto.setIsActive(mode.isActive());
		return dto;
	}

	public TherapistClientsDto assembleEntityToDto(TherapistClients therapistClients) {
		TherapistClientsDto therapistClientsDto = new TherapistClientsDto();
		therapistClientsDto.setTherapistId(therapistClients.getTherapistId());
		therapistClientsDto.setClientId(therapistClients.getClientId());
		therapistClientsDto.setClientName(therapistClients.getClientName());
		therapistClientsDto.setDsf(therapistClients.isDsf());

		return therapistClientsDto;
	}

	public SessionNotes assembleDtoToEntity(SessionNotesDto sessionNotesDto) {
		SessionNotes sessionNotes = new SessionNotes();
		sessionNotes.setAppointmentId(sessionNotesDto.getAppointmentId());
		sessionNotes.setTherapistId(sessionNotesDto.getTherapistId());
		sessionNotes.setCreatedAt(LocalDateTime.now());
		sessionNotes.setClientId(null);
		sessionNotes.setNoteContent(sessionNotesDto.getSessionNotes());

		return sessionNotes;
	}

	public ClientNotesDto assembleEntityToDto(ClientNotes clientNotes) {
		ClientNotesDto clientNotesDto = new ClientNotesDto();
		clientNotesDto.setClientId(clientNotes.getClientId());
		clientNotesDto.setTherapistId(clientNotes.getTherapistId());
		clientNotesDto.setContent(clientNotes.getContent());
		clientNotesDto.setCreatedAt(clientNotes.getCreatedAt());
		clientNotesDto.setUpdatedAt(clientNotes.getUpdatedAt());
		clientNotesDto.setNoteId(clientNotes.getNoteId());

		return clientNotesDto;
	}

	public ClientNotes assembleDtoToEntity(ClientNotesDto clientNotesDto) {
		ClientNotes clientNotes = new ClientNotes();
		clientNotes.setClientId(clientNotesDto.getClientId());
		clientNotes.setTherapistId(clientNotesDto.getTherapistId());
		clientNotes.setContent(clientNotesDto.getContent());

		return clientNotes;
	}

}
