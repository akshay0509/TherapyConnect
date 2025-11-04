package com.org.therapistService.Entity;

import java.time.LocalDateTime;

import com.org.therapistService.Enums.SessionType;

import lombok.Data;

@Data
public class TherapistAvailabilityDto {

	private String slotId;
	private String therapistId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private SessionType sessionType;
	private String serviceId;
}
