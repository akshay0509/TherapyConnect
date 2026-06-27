package com.org.therapistService.Dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ClientNotesDto {

	private String noteId;
	private String clientId;
	private String therapistId;
	private String content;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
