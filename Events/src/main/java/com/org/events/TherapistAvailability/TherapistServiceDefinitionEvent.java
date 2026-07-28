package com.org.events.TherapistAvailability;

import java.util.UUID;

import lombok.Data;

/**
 * Replicates the booking-relevant part of a therapist service into
 * AppointmentService. Availability blocks deliberately do not carry this data.
 */
@Data
public class TherapistServiceDefinitionEvent {

	private String eventId;
	private String eventType;
	private String serviceId;
	private String therapistId;
	private Integer durationMinutes;
	private Boolean isActive;

	public TherapistServiceDefinitionEvent() {
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
		this.eventId = "EVNT" + uniquePart;
	}
}
