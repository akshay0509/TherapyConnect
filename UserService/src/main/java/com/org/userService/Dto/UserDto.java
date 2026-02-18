package com.org.userService.Dto;

import com.org.userService.Enum.UserRole;

import lombok.Data;

@Data
public class UserDto {

	private String username;
	private String email;
	private String password;
	private UserRole userRole;
}
