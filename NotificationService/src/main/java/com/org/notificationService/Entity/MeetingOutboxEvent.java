package com.org.notificationService.Entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data @Table(name = "MEETING_OUTBOX_EVENT")
public class MeetingOutboxEvent {
    @Id private String eventId;
    private String appointmentId;
    @Column(columnDefinition = "text", nullable = false) private String payload;
    private LocalDateTime createdAt;
    private boolean published;
    private int retryCount;
    private boolean parked;
}
