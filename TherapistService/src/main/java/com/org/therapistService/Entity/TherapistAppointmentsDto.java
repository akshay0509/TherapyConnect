package com.org.therapistService.Entity;

import lombok.Data;

@Data
public class TherapistAppointmentsDto {

	private String therapistId;
	private String clientId;
	private String serviceId;
	private String slotId;
	
}
