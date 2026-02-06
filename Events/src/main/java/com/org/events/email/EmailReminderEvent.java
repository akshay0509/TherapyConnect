package com.org.events.email;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EmailReminderEvent{
	String email;
	LocalDateTime appointmentTime;
}
