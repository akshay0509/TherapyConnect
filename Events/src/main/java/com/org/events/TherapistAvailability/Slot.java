package com.org.events.TherapistAvailability;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Slot {

	private String slotId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
