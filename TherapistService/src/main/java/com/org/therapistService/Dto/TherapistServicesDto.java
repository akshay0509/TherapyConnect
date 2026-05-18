package com.org.therapistService.Dto;

import java.math.BigDecimal;

import com.org.therapistService.Enums.ServiceType;

import lombok.Data;

@Data
public class TherapistServicesDto {

	private String serviceId;
	private String therapistId;
	private ServiceType serviceType;
	private int duration;
	private BigDecimal price;
	private Boolean isActive;
}
