package com.org.notificationService.Dto;

import java.sql.Date;
import java.sql.Time;

import lombok.Data;

@Data
public class ClientReminderDto {
	
	private String email;
	private Date AppointmentDate;
	private Time AppointmentTime;
}
