package com.org.userService.Dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {

	private String token;
	private String newPassword;
}
