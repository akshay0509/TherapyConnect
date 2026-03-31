package com.org.appointmentService.Dto;

import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {

    @NotNull
    private AppointmentStatus status;

    @NotNull
    private String appointmentId;
    
    private String therapistId;
    
    private String reason;
}
