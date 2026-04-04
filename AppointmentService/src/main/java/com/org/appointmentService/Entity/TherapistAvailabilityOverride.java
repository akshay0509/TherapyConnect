package com.org.appointmentService.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "THERAPIST_AVAILABILITY_OVERRIDE")
public class TherapistAvailabilityOverride {

    @Id
    private String overrideId;

    private String therapistId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
    private String reason;
}
