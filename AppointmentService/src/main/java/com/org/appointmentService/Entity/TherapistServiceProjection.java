package com.org.appointmentService.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_SERVICE_PROJECTION")
public class TherapistServiceProjection {

	@Id
	private String serviceId;

	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private int durationMinutes;

	@Column(nullable = false)
	private boolean active;
}
