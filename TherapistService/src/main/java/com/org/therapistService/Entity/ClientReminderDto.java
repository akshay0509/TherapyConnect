package com.org.therapistService.Entity;

import java.sql.Date;
import java.sql.Time;

import lombok.Data;

@Data
public class ClientReminderDto {
	
	private String email;
	private Date AppointmentDate;
	private Time AppointmentTime;
}
