package com.org.therapistService.Assembler;

import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistAppointments;
import com.org.therapistService.Entity.TherapistAppointmentsDto;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityDto;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistAvailabilityRulesDto;
import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Entity.TherapistServicesDto;

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
		
		return therapistAvailabilityOverrides;
	}
	
	public TherapistAvailabilityOverridesDto assembleEntityToDto(TherapistAvailabilityOverrides therapistAvailabilityOverrides) {
		TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto = new TherapistAvailabilityOverridesDto();
		therapistAvailabilityOverridesDto.setOverrideId(therapistAvailabilityOverrides.getOverrideId());
		therapistAvailabilityOverridesDto.setTherapistId(therapistAvailabilityOverrides.getTherapistId());
		therapistAvailabilityOverridesDto.setStartTime(therapistAvailabilityOverrides.getStartTime());
		therapistAvailabilityOverridesDto.setEndTime(therapistAvailabilityOverrides.getEndTime());
		therapistAvailabilityOverridesDto.setIsAvailable(therapistAvailabilityOverrides.isAvailable());
		
		return therapistAvailabilityOverridesDto;
	}
	
	public TherapistAvailability assembleDtoToEntity(TherapistAvailabilityDto therapistAvailabilityDto) {
		TherapistAvailability therapistAvailability = new TherapistAvailability();
		therapistAvailability.setTherapistId(therapistAvailabilityDto.getTherapistId());
		therapistAvailability.setSessionType(therapistAvailabilityDto.getSessionType());
		therapistAvailability.setStartTime(therapistAvailabilityDto.getStartTime());
		therapistAvailability.setEndTime(therapistAvailabilityDto.getEndTime());
		therapistAvailability.setServiceId(therapistAvailabilityDto.getServiceId());
		
		return therapistAvailability;
	}
	
	public TherapistAvailabilityDto assembleEntityToDto(TherapistAvailability therapistAvailability) {
		TherapistAvailabilityDto therapistAvailabilityDto = new TherapistAvailabilityDto();
		therapistAvailabilityDto.setSlotId(therapistAvailability.getSlotId());
		therapistAvailabilityDto.setTherapistId(therapistAvailability.getTherapistId());
		therapistAvailabilityDto.setSessionType(therapistAvailability.getSessionType());
		therapistAvailabilityDto.setStartTime(therapistAvailability.getStartTime());
		therapistAvailabilityDto.setEndTime(therapistAvailability.getEndTime());
		therapistAvailabilityDto.setServiceId(therapistAvailability.getServiceId());
		
		return therapistAvailabilityDto;
	}
	
	public TherapistAppointments assembleDtoToEntity(TherapistAppointmentsDto therapistAppointmentsDto) {
		TherapistAppointments therapistAppointments = new TherapistAppointments();
		therapistAppointments.setTherapistId(therapistAppointmentsDto.getTherapistId());
		therapistAppointments.setClientId(therapistAppointmentsDto.getClientId());
		therapistAppointments.setSlotId(therapistAppointmentsDto.getSlotId());
		therapistAppointments.setServiceId(therapistAppointmentsDto.getServiceId());
		therapistAppointments.setStartTime(therapistAppointmentsDto.getStartTime());
		therapistAppointments.setEndTime(therapistAppointmentsDto.getEndTime());
		
		return therapistAppointments;
	}
	
	public TherapistAppointmentsDto assembleEntityToDto(TherapistAppointments therapistAppointments) {
		TherapistAppointmentsDto therapistAppointmentsDto = new TherapistAppointmentsDto();
		therapistAppointmentsDto.setAppointmentId(therapistAppointments.getAppointmentId());
		therapistAppointmentsDto.setTherapistId(therapistAppointments.getTherapistId());
		therapistAppointmentsDto.setClientId(therapistAppointments.getClientId());
		therapistAppointmentsDto.setSlotId(therapistAppointments.getSlotId());
		therapistAppointmentsDto.setServiceId(therapistAppointments.getServiceId());
		therapistAppointmentsDto.setStartTime(therapistAppointments.getStartTime());
		therapistAppointmentsDto.setEndTime(therapistAppointments.getEndTime());
		therapistAppointmentsDto.setStatus(therapistAppointments.getStatus());
		therapistAppointmentsDto.setReminderSent(therapistAppointments.isReminderSent());
		
		return therapistAppointmentsDto;
	}
	
}
