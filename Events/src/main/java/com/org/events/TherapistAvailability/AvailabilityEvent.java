package com.org.events.TherapistAvailability;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AvailabilityEvent {

	private String eventType;
    private String slotId;
    private String therapistId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String generationId;
}
