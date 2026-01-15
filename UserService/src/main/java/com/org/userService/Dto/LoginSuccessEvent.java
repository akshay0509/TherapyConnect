package com.org.userService.Dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class LoginSuccessEvent {

	private UUID userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private Instant timestamp;
}
