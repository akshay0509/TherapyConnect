package com.org.therapistService.Entity;

import com.org.therapistService.Enums.ServiceType;

import lombok.Data;

@Data
public class TherapistServicesDto {

	private String serviceId;
	private String therapistId;
	private ServiceType serviceType;
	private int duration;
	private float price;
	private boolean isActive;
}
