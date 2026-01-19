package com.org.userService.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.org.userService.Dto.AuthRequest;
import com.org.userService.Dto.AuthResponse;
import com.org.userService.Dto.UserDto;
import com.org.userService.Services.UserService;

@RestController
public class UserController {
	
	@Autowired
	UserService userService;
	
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@PostMapping("/create-user")
	public void createUser(@RequestBody UserDto userDto) {
		userService.createUser(userDto);
	}
	
	@PostMapping("/validate-user")
	public ResponseEntity<AuthResponse> validate(@RequestBody AuthRequest authRequest){
		logger.debug("inside validate user "+authRequest);
		AuthResponse authResponse = userService.validateUser(authRequest);
		logger.debug("exiting validate user "+authResponse);
		return ResponseEntity.ok(authResponse);
	}
	
}
