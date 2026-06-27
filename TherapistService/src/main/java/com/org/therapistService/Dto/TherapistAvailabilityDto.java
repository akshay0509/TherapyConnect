package com.org.therapistService.Dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TherapistAvailabilityDto {

	private String slotId;
	private String therapistId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String serviceId;
	private List<SlotDeliveryOptionDto> deliveryOptions;
}
