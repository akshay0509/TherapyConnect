package com.org.therapistService.Entity;

import java.time.LocalDateTime;

import com.org.therapistService.Enums.AppointmentStatus;

import lombok.Data;

@Data
public class TherapistAppointmentsDto {

	private String appointmentId;
	private String therapistId;
	private String clientId;
	private String serviceId;
	private String slotId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private AppointmentStatus status;
	private Boolean reminderSent;
	
}
