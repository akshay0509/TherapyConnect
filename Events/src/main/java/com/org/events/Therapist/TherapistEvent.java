package com.org.events.Therapist;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class TherapistEvent {

	private String eventId;
	private String eventType;
	private LocalDateTime occurredAt;
	private String therapistId;
	private String timezone;
	private String email;
	private Boolean paymentEnabled;

	public TherapistEvent() {
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
		this.eventId = "EVNT" + uniquePart;
		this.occurredAt = LocalDateTime.now();
	}
}
