package com.org.userService.Dto;

import java.util.Set;

import com.org.userService.Enum.FailureReason;

import lombok.Data;

@Data
public class AuthResponse {

	private boolean authenticated;
	private String userId;
	private String username;
	private Set<String> roles;
	private AccountStatus accountStatus;
	private FailureReason failureReason;

	public static AuthResponse success(String userId, String username, Set<String> roles, boolean enabled, boolean locked) {
		AuthResponse response = new AuthResponse();
		response.authenticated = true;
		response.userId = userId;
		response.username = username;
		response.roles = roles;
		response.accountStatus = new AccountStatus(enabled, locked);
		return response;
	}

	public static AuthResponse failure(FailureReason failureReason) {
		AuthResponse response = new AuthResponse();
		response.authenticated = false;
		response.failureReason = failureReason;
		return response;
	}

	public static class AccountStatus {
		private boolean enabled;
		private boolean locked;

		public AccountStatus(boolean enabled, boolean locked) {
			this.enabled = enabled;
			this.locked = locked;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean isLocked() {
			return locked;
		}
	}
}
