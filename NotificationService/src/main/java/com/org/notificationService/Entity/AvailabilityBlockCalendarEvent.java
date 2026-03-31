package com.org.notificationService.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "AVAILABILITY_BLOCK_CALENDAR_EVENT")
public class AvailabilityBlockCalendarEvent {

    @Id
    private String blockId;

    @Column(nullable = false)
    private String googleCalendarEventId;
}
