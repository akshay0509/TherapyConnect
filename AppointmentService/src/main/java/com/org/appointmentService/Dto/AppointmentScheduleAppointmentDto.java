package com.org.appointmentService.Dto;

import java.time.LocalDateTime;

import com.org.appointmentService.Enums.PaymentStatus;
import com.org.events.TherapistAppointment.AppointmentStatus;

import lombok.Data;

@Data
public class AppointmentScheduleAppointmentDto {

    private String appointmentId;
    private String clientId;
    private String clientName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private String modeId;
    private String reason;

    /** null when no payment applies to this appointment */
    private PaymentStatus paymentStatus;
    private String paymentLinkUrl;
}
