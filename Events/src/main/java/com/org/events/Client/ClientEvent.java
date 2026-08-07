package com.org.events.Client;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class ClientEvent {

	private String eventId;
	private String eventType;
	private LocalDateTime occurredAt;
	private String clientId;
	private String therapistId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String fullName;
	// Negotiated per-client rate. Null means the client has no special rate
	// and the delivery-mode price applies.
	private java.math.BigDecimal sessionFee;
    private ClientStatus status;
    private boolean dsf;
    
    public ClientEvent(){
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
	}
}
