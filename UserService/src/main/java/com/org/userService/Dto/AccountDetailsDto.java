package com.org.userService.Dto;

import java.time.Instant;

import com.org.userService.Enum.UserRole;

import lombok.Data;

@Data
public class AccountDetailsDto {

	private String username;
	private String email;
	private UserRole userRole;
	private Instant createdAt;
	private Instant lastLoginTime;
	private String lastLoginIp;
	private String lastLoginUserAgent;
}
