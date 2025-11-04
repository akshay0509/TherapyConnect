package com.org.therapistService.Entity;

import java.time.LocalTime;

import lombok.Data;

@Data
public class TherapistAvailabilityRulesDto {

	private String ruleId;
	private String therapistId;
	private LocalTime dayOfWeek;
	private LocalTime startTime;
	private LocalTime endTime;
	private boolean isActive;
	
}
