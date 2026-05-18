package com.org.events.TherapistAvailability;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AvailabilityEvent {

	private String eventId;
	private String eventType;
    private String slotId;
    private String therapistId;
    private BigDecimal sessionFee;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String generationId;
    
    public AvailabilityEvent(){
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
	}
}
