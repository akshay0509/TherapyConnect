package com.org.therapistService.Entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ClientReminderDto {
	
	private String email;
	private LocalDateTime AppointmentTime;
}
