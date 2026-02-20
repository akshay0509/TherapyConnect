package com.org.appointmentService.Dto;

import com.org.appointmentService.Enums.SessionType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookAppointmentRequest {

	@NotNull
	private String slotId;
	
	@NotNull
	private String therapistId;
	
	@NotNull
	private String clientId;
	
	private String clientName;
	private SessionType sessionType;
}
