package com.org.therapistService.Dto;

import java.time.LocalDateTime;

import com.org.events.Client.ClientStatus;

import lombok.Data;

@Data
public class TherapistClientsDto {

	private String therapistId;
	private String clientId;
	private String clientName;
	private boolean dsf;

	// Already persisted on TherapistClients — previously just not exposed.
	private ClientStatus status;
	private LocalDateTime createdAt;

	// Aggregates over completed appointments, filled from one grouped query.
	private long sessionCount;
	private LocalDateTime lastSeen;
	private long pendingNotes;
}
