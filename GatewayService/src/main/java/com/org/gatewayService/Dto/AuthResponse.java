package com.org.gatewayService.Dto;

import java.util.Set;

import com.org.gatewayService.Enum.FailureReason;

import lombok.Data;

@Data
public class AuthResponse {

	private boolean authenticated;
	private String userId;
	private String username;
	private Set<String> roles;
	private AccountStatus accountStatus;
	private FailureReason failureReason;

	@Data
	public static class AccountStatus {
		private boolean enabled;
		private boolean locked;
	}
}
