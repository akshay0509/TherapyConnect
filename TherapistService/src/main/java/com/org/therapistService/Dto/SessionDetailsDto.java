package com.org.therapistService.Dto;

import java.math.BigDecimal;
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
	// Captured fee for this booking. Lets the client page total "paid" the same
	// way it already derives session count and attendance — from this list —
	// rather than waiting on a separate payments endpoint.
	private BigDecimal sessionFee;
}
