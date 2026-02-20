package com.org.events.TherapistAvailability;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AvailabilitySlotsDeletedEvent {

	private String eventId;
	private LocalDateTime occurredAt;
	private String eventType = "AvailabilitySlotsDeleted";
	private String therapistId;
	private LocalDate rangeStart;
    private LocalDate rangeEnd;
    
    public AvailabilitySlotsDeletedEvent(){
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
        this.occurredAt = LocalDateTime.now();
	}
}
