package com.org.events.TherapistAvailability;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class DeliveryModeEvent {

    private String eventId;
    private String eventType;

    private String modeId;
    private String therapistId;
    private String serviceId;
    private String modeType;
    private String displayName;
    private String address;
    private BigDecimal price;
    private Boolean isActive;

    public DeliveryModeEvent() {
        String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
    }
}
