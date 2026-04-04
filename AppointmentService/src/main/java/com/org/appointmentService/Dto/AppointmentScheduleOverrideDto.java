package com.org.appointmentService.Dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AppointmentScheduleOverrideDto {

    private String overrideId;
    private String therapistId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
    private String reason;
}
