package com.org.userService.Controller;

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

	@PostMapping("/create-user")
	public void createUser(@RequestBody UserDto userDto) {
		userService.createUser(userDto);
	}
	
	@PostMapping("/validate-user")
	public ResponseEntity<AuthResponse> validate(@RequestBody AuthRequest authRequest){
		AuthResponse authResponse = userService.validateUser(authRequest);
		
		if (!authResponse.isAuthenticated()) {
            return ResponseEntity.status(401).body(authResponse);
        }

        return ResponseEntity.ok(authResponse);
	}
	
}
