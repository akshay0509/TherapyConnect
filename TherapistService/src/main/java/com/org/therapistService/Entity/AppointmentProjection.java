package com.org.therapistService.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.persistence.Entity;
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

    private LocalDateTime startTime;

    private LocalDateTime endTime;
    
    private BigDecimal sessionFee;

    private AppointmentStatus status;

    private LocalDateTime updatedAt;
    
    private String sessionType;
}
