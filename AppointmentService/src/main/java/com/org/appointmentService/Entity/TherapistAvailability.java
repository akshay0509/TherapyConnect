package com.org.appointmentService.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.org.appointmentService.Enums.AvailabilityStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_AVAILABILITY", uniqueConstraints = {
		@UniqueConstraint(
				name = "uk_appointment_availability_block",
				columnNames = {"therapistId", "startTime", "endTime"})
})
public class TherapistAvailability {

	@Id
	private String slotId;
	
	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;
	
	@Column(nullable = true)
	private String serviceId;
	
	@Column(nullable = true)
	private BigDecimal sessionFee;

	/**
	 * Reservation owner. Every 30-minute block occupied by a multi-block
	 * appointment carries the same appointmentId.
	 */
	@Column(nullable = true)
	private String appointmentId;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;
	
}
