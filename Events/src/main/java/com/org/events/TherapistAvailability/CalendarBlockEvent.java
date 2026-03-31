package com.org.events.TherapistAvailability;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class CalendarBlockEvent {

    private String eventId;
    private String eventType; // CODEX-CALENDAR-BLOCKING
    private String blockId; // CODEX-CALENDAR-BLOCKING
    private String therapistId;
    private LocalDateTime startTime; // CODEX-CALENDAR-BLOCKING
    private LocalDateTime endTime; // CODEX-CALENDAR-BLOCKING
    private String reason; // CODEX-CALENDAR-BLOCKING
    private Boolean syncToGoogleCalendar; // CODEX-CALENDAR-BLOCKING
    private LocalDateTime occurredAt; // CODEX-CALENDAR-BLOCKING

    public CalendarBlockEvent() {
        String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
        this.occurredAt = LocalDateTime.now(); // CODEX-CALENDAR-BLOCKING
    }
}
