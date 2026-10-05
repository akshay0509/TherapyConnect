package com.org.notificationService.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity @Data @Table(name = "APPOINTMENT_MEETING_STATE")
public class AppointmentMeetingState {
    @Id private String appointmentId;
    private String therapistId;
    private String googleCalendarEventId;
    private Long calendarRevision = 0L;
    private Long meetingSequence = 0L;
    private String meetingStatus;
    @Column(length = 2048) private String meetingUrl;
    private Integer retryCount = 0;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime updatedAt;
    // Retained after cancellation: stale events must not recreate deleted meetings.
}
