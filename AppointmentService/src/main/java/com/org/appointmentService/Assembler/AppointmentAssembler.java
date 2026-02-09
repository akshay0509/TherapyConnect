package com.org.appointmentService.Assembler;

import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Entity.TherapistAppointmentsDto;

public class AppointmentAssembler {

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
