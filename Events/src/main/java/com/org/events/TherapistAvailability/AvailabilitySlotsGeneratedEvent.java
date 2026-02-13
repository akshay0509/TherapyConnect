package com.org.events.TherapistAvailability;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class AvailabilitySlotsGeneratedEvent {

	private String eventType;
    private String eventId;
    private LocalDateTime occurredAt;

    private String generationId;
    private String therapistId;

    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    private List<Slot> slotList;
    
    public AvailabilitySlotsGeneratedEvent(){
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
        this.generationId = "GEN" + uniquePart;
        this.occurredAt = LocalDateTime.now();
	}
}
