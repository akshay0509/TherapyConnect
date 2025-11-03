package com.org.therapistService.Entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TherapistAvailabilityOverridesDto {

	private String therapistId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private boolean isAvailable;
	
}
