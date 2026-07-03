package com.org.userService.Controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.org.userService.Dto.AuthRequest;
import com.org.userService.Dto.AuthResponse;
import com.org.userService.Dto.ForgotPasswordRequest;
import com.org.userService.Dto.ResetPasswordRequest;
import com.org.userService.Dto.UpdateAccountRequest;
import com.org.userService.Dto.UserDto;
import com.org.userService.Services.UserService;

@RestController
public class UserController {

	@Autowired
	UserService userService;

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@PostMapping("/create-user")
	public ResponseEntity<Map<String, String>> createUser(@RequestBody UserDto userDto) {
		try {
			userService.createUser(userDto);
			return ResponseEntity.ok(Map.of("message", "User created successfully."));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/validate-user")
	public ResponseEntity<AuthResponse> validate(@RequestBody AuthRequest authRequest){
		logger.debug("inside validate user "+authRequest);
		AuthResponse authResponse = userService.validateUser(authRequest);
		logger.debug("exiting validate user "+authResponse);
		return ResponseEntity.ok(authResponse);
	}

	@PostMapping("/forgot-username")
	public ResponseEntity<String> forgotUsername(@RequestBody ForgotPasswordRequest request) {
		userService.sendUsername(request.getEmail());
		return ResponseEntity.ok("If an account with that email exists, your username has been sent.");
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
		userService.createPasswordResetToken(request.getEmail());
		return ResponseEntity.ok("If the email exists, reset instructions will be sent.");
	}

	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
		userService.resetPassword(request.getToken(), request.getNewPassword());
		return ResponseEntity.ok("Password reset successfully.");
	}

	@PutMapping("/update-account")
	public ResponseEntity<UserDto> updateAccount(@RequestBody UpdateAccountRequest request) {
		return ResponseEntity.ok(userService.updateAccount(request));
	}

}
