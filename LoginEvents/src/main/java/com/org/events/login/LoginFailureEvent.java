package com.org.events.login;

import java.time.Instant;

import lombok.Data;

@Data
public class LoginFailureEvent {

	private String userId;
	private String username;
	private String ipAddress;
	private String userAgent;
	private String reason;
	private Instant timestamp;
}
