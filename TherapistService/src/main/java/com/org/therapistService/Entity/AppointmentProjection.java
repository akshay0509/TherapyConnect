package com.org.therapistService.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "APPOINTMENT_PROJECTION")
public class AppointmentProjection {

	@Id
    private String appointmentId;

    private String therapistId;

    private String clientId;

    private String serviceId;

    private String modeId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
    
    private BigDecimal sessionFee;

    /* STRING, not the JPA default ORDINAL. This column stored positions while
       the source table (TherapistAppointments) stored names, and the enum was
       reordered on 01 Apr 2026 (f03831d) — so any row written before that date
       reads back shifted, with the old COMPLETED (1) surfacing as CANCELLED.
       Client Detail reads this table; the Schedule page reads the source, which
       is how the same appointment could show two different statuses. */
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private LocalDateTime updatedAt;
    
}
