package com.org.therapistService.Entity;

import lombok.Data;

@Data
public class SessionNotesDto {

	private String noteId;
	private String therapistId;
	private String appointmentId;
	private String clientId;
	private String noteContent;
}
