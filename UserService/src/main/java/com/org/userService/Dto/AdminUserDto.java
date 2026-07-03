package com.org.userService.Dto;

import java.time.Instant;

import com.org.userService.Enum.UserRole;

import lombok.Data;

@Data
public class AdminUserDto {

	private String userId;
	private String username;
	private String email;
	private UserRole userRole;
	private boolean enabled;
	private boolean accountLocked;
	private int failedAttempts;
	private Instant lastLoginTime;
	private Instant createdAt;
}
