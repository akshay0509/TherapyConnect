package com.org.therapistService.Dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TherapistAvailabilityOverridesDto {

	private String overrideId;
	private String therapistId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Boolean isAvailable;
	private String reason;
    private Boolean syncToGoogleCalendar;
	
}
