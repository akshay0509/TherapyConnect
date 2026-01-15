package com.org.userService.Dto;

import java.time.Instant;

import lombok.Data;

@Data
public class LoginFailureEvent {

	private String username;
	private String ipAddress;
	private String userAgent;
	private String reason;
	private Instant timestamp;
}
