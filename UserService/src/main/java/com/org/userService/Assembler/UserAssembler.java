package com.org.userService.Assembler;

import com.org.userService.Dto.UserDto;
import com.org.userService.Entity.User;

public class UserAssembler {

	public User assembleDtoToEntity(UserDto userDto) {
		User user = new User();
		user.setUsername(userDto.getUsername());
		user.setEmail(userDto.getEmail());
		user.setUserRole(userDto.getUserRole());
		return user;
	}
}
