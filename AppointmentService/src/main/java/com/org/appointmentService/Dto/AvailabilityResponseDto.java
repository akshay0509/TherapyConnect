package com.org.appointmentService.Dto;

import java.time.LocalDateTime;

import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Enums.SessionType;
import com.org.events.TherapistAppointment.AppointmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilityResponseDto {

	private String slotId;
	private String therapistId;
	private String serviceId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private SessionType sessionType;
	private AvailabilityStatus slotStatus;
	private AppointmentStatus appointmentStatus;
	private String appointmentId;
	private String clientId;
	private String clientName;
}
