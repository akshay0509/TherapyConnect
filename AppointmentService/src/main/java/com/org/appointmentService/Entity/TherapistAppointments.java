package com.org.appointmentService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.org.appointmentService.Enums.AppointmentStatus;
import com.org.appointmentService.Enums.Creator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_APPOINTMENTS", uniqueConstraints = {
	    // CRITICAL: Prevents double-booking by ensuring only one appointment 
	    // can exist for a specific time slot (slot_id) where the appointment 
	    // status is NOT 'CANCELLED'.
	    @UniqueConstraint(columnNames = {"slotId"}) 
	})
public class TherapistAppointments {

	@Id
	private String appointmentId;
	
	@Column(nullable = false)
	private String therapistId;
	
	@Column(nullable = false)
	private String clientId;
	
	@Column(nullable = false)
	private String serviceId;
	
	@Column(nullable = false, unique = true)
	private String slotId;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;
	
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
