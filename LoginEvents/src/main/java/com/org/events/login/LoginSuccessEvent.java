package com.org.events.login;

import java.time.Instant;

import lombok.Data;

@Data
public class LoginSuccessEvent {

	private String userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private Instant timestamp;
}
