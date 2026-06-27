package com.org.appointmentService.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.org.appointmentService.Enums.Creator;
import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_APPOINTMENTS")
public class TherapistAppointments {

	@Id
	private String appointmentId;
	
	@Column(nullable = false)
	private String therapistId;
	
	@Column(nullable = false)
	private String clientId;
	
	@Column(nullable = false)
	private String clientName;
	
	@Column(nullable = false)
	private String slotId;
	
	@Column(nullable = true)
	private String modeId;
	
	@Column(nullable = true)
	private BigDecimal sessionFee;
	
	@Column(nullable = true)
	private String statusReason;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "createdBy", nullable = false)
    private Creator createdBy = Creator.THERAPIST;

	private boolean reminderSent = false;
	
	@PrePersist
    public void generateId() {
        if (this.appointmentId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.appointmentId = "APP" + uniquePart;
        }
    }
}
