package com.org.userService.Services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.org.userService.Assembler.UserAssembler;
import com.org.userService.Dto.AuthRequest;
import com.org.userService.Dto.AuthResponse;
import com.org.userService.Dto.UpdateAccountRequest;
import com.org.userService.Dto.UserDto;
import com.org.userService.Entity.User;
import com.org.userService.Enum.FailureReason;
import com.org.userService.Repository.UserRepository;
import com.org.userService.Utility.SecurityUtils;
import com.org.userService.Services.EmailService;

@Service
public class UserService {

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	EmailService emailService;

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);
	
	private UserAssembler userAssembler = new UserAssembler();
	
	public void createUser(UserDto userDto) {
		logger.debug("inside createUser.");
		User user = userAssembler.assembleDtoToEntity(userDto);
		
		String passwordHash = passwordEncoder.encode(userDto.getPassword());
		user.setPasswordHash(passwordHash);
		
		userRepository.save(user);
		logger.debug("exiting createUser.");
	}
	
	public AuthResponse validateUser(AuthRequest authRequest) {
		User user = userRepository.findByUsername(authRequest.getUsername());
		
		if (user == null) {
            return AuthResponse.failure(FailureReason.INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            return AuthResponse.failure(FailureReason.ACCOUNT_DISABLED);
        }

        if (user.isAccountLocked()) {
            return AuthResponse.failure(FailureReason.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPasswordHash())) {
            return AuthResponse.failure(FailureReason.INVALID_CREDENTIALS);
        }
		
        return AuthResponse.success(
                user.getUserId(),
                user.getUsername(),
                Set.of(user.getUserRole().toString()),
                user.isEnabled(),
                user.isAccountLocked()
        );
		
	}
	
	public void createPasswordResetToken(String email) {
		User user = userRepository.findByEmail(email);
		if (user == null) {
			return;
		}

		String token = UUID.randomUUID().toString();
		user.setResetPasswordToken(token);
		user.setResetPasswordTokenExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
		userRepository.save(user);

		emailService.sendPasswordResetEmail(email, token);
	}

	public void resetPassword(String token, String newPassword) {
		User user = userRepository.findByResetPasswordToken(token);
		if (user == null || user.getResetPasswordTokenExpiresAt() == null || user.getResetPasswordTokenExpiresAt().isBefore(Instant.now())) {
			throw new IllegalArgumentException("Invalid or expired reset token.");
		}

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setResetPasswordToken(null);
		user.setResetPasswordTokenExpiresAt(null);
		userRepository.save(user);
	}

	public UserDto updateAccount(UpdateAccountRequest request) {
		// Username resolved from the validated JWT — not from the request body
		String lookupUsername = SecurityUtils.getUsername();
		User user = userRepository.findByUsername(lookupUsername);
		if (user == null) {
			throw new IllegalArgumentException("User not found.");
		}

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("Current password is invalid.");
		}

		if (request.getEmail() != null && !request.getEmail().isBlank()) {
			user.setEmail(request.getEmail());
		}
		if (request.getUsername() != null && !request.getUsername().isBlank()) {
			user.setUsername(request.getUsername());
		}

		userRepository.save(user);

		UserDto dto = new UserDto();
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setUserRole(user.getUserRole());
		return dto;
	}
}
