package com.org.appointmentService.Dto;

import java.math.BigDecimal;

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

	@NotNull
	private String modeId;

	private String clientName;

	// Optional fee override. When set, replaces the mode's default price for this appointment.
	private BigDecimal customPrice;
}
