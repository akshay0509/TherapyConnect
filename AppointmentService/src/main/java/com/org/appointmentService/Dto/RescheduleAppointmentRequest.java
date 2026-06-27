package com.org.appointmentService.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RescheduleAppointmentRequest {

	@NotNull
    private String newSlotId;
	
	@NotNull
    private String appointmentId;
    
    private String therapistId;

    private String reason;
    private String modeId;
}
