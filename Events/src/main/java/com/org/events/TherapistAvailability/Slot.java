package com.org.events.TherapistAvailability;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Slot {

	private String slotId;
	private String serviceId;
	private String modeId;
	private BigDecimal sessionFee;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
