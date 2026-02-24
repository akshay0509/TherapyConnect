package com.org.therapistService.Dto;

import lombok.Data;

@Data
public class SessionNotesDto {

	private String noteId;
	private String therapistId;
	private String appointmentId;
	private String clientId;
	private String sessionNotes;
}
