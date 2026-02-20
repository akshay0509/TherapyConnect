package com.org.appointmentService.Dto;

import java.time.LocalDateTime;

import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Enums.SessionType;

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
	private AvailabilityStatus status;
	private String appointmentId;
	private String clientId;
	private String clientName;
}
