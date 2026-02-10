package com.org.events.TherapistAppointment;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AppointmentEvent {

	private String eventId;
	private String eventType;
	private String appointmentId;
	private String slotId;
	private String therapistId;
	private String clientId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String bookingSource;
	
	private String oldSlotId;
	private String newSlotId;
	private LocalDateTime oldStartTime;
	private LocalDateTime oldEndTime;
	
	private String cancelledBy;
	private String reason;
	
	public AppointmentEvent(){
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
	}
}
