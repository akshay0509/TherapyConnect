package com.org.therapistService.Dto;

import java.time.LocalTime;

import lombok.Data;

@Data
public class TherapistAvailabilityRulesDto {

	private String ruleId;
	private String therapistId;
	private int dayOfWeek;
	private LocalTime startTime;
	private LocalTime endTime;
	private Boolean isActive;
	
}
