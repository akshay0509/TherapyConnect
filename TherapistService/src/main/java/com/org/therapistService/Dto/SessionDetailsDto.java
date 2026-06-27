package com.org.therapistService.Dto;

import java.time.LocalDateTime;

import com.org.events.TherapistAppointment.AppointmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionDetailsDto {

	private String appointmentId;
	private String clientId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private AppointmentStatus status;
	private String modeId;
	private String sessionNotes;
}
