package com.org.userService.Dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {

	private String currentPassword;
	private String newPassword;
}
