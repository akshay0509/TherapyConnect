package com.org.therapistService.Dto;

import java.time.LocalTime;

import com.org.therapistService.Enums.SessionType;

import lombok.Data;

@Data
public class TherapistAvailabilityRulesDto {

	private String ruleId;
	private String therapistId;
	private int dayOfWeek;
	private LocalTime startTime;
	private LocalTime endTime;
	/*
	private SessionType sessionType;
	*/
	private Boolean isActive;
	
}
