package com.org.notificationService.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "APPOINTMENT_CALENDAR_EVENT")
public class AppointmentCalendarEvent {

    @Id
    private String appointmentId;

    @Column(nullable = false)
    private String googleCalendarEventId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
    }

}
