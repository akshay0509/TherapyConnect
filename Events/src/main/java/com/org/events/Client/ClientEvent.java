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
    private String email;
    private String firstName;
    private String lastName;
    private ClientStatus status;
    
    public ClientEvent(){
		String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        this.eventId = "EVNT" + uniquePart;
	}
}
