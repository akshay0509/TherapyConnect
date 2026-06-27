package com.org.userService.Dto;

import lombok.Data;

@Data
public class UpdateAccountRequest {

	private String currentUsername;
	private String username;
	private String email;
	private String currentPassword;
}
