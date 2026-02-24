package com.org.appointmentService.Entity;

import java.time.LocalDateTime;

import com.org.appointmentService.Enums.AvailabilityStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_AVAILABILITY")
public class TherapistAvailability {

	@Id
	private String slotId;
	
	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;
	
	/*
	@Enumerated(EnumType.STRING)
	private SessionType sessionType;
	*/
	
	@Column(nullable = true)
	private String serviceId;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;
	
}
