package com.org.events.TherapistAvailability;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AvailabilityOverrideEvent {

    private String eventType;
    private String eventId;
    private String overrideId;
    private String therapistId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
    private String reason;
    private LocalDateTime occurredAt;

    public AvailabilityOverrideEvent() {
        String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
        this.occurredAt = LocalDateTime.now();
    }
}

