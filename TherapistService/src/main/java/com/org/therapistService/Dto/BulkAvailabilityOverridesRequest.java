package com.org.therapistService.Dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BulkAvailabilityOverridesRequest {

	private LocalDate startDate;
	private LocalDate endDate;
	private Boolean isAvailable = false;
	private String reason;
	private Boolean syncToGoogleCalendar = false;
}
