package com.org.userService.Services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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

	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
	private static final String USERNAME_RULES =
			"Username must be 3-30 characters using only letters, numbers, dots, dashes or underscores (no spaces).";

	private UserAssembler userAssembler = new UserAssembler();

	public void createUser(UserDto userDto) {
		logger.debug("inside createUser.");

		validateNewUser(userDto);

		if (userRepository.existsByEmail(userDto.getEmail())) {
			throw new IllegalArgumentException("An account with this email already exists.");
		}
		if (userRepository.existsByUsername(userDto.getUsername())) {
			throw new IllegalArgumentException("That username is already taken.");
		}

		User user = userAssembler.assembleDtoToEntity(userDto);
		String passwordHash = passwordEncoder.encode(userDto.getPassword());
		user.setPasswordHash(passwordHash);
		userRepository.save(user);
		logger.debug("exiting createUser.");
	}

	// /create-user is unauthenticated — these rules must live server-side,
	// not only in the frontend form
	private void validateNewUser(UserDto userDto) {
		if (userDto.getUsername() == null || userDto.getUsername().isBlank()) {
			throw new IllegalArgumentException("Username is required.");
		}
		// accidental surrounding whitespace is trimmed rather than rejected;
		// anything else (inner spaces, odd characters) fails the pattern
		userDto.setUsername(userDto.getUsername().trim());
		validateUsername(userDto.getUsername());
		if (userDto.getEmail() == null || !EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
			throw new IllegalArgumentException("A valid email address is required.");
		}
		validatePasswordPolicy(userDto.getPassword());
	}

	private void validateUsername(String username) {
		if (!USERNAME_PATTERN.matcher(username).matches()) {
			throw new IllegalArgumentException(USERNAME_RULES);
		}
	}

	private void validatePasswordPolicy(String password) {
		if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
		}
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

	public void sendUsername(String email) {
		User user = userRepository.findByEmail(email);
		if (user == null) {
			return;
		}
		emailService.sendUsernameEmail(email, user.getUsername());
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

		validatePasswordPolicy(newPassword);

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setResetPasswordToken(null);
		user.setResetPasswordTokenExpiresAt(null);
		userRepository.save(user);
	}

	public UserDto updateAccount(UpdateAccountRequest request) {
		String lookupUsername = SecurityUtils.getUsername();
		User user = userRepository.findByUsername(lookupUsername);
		if (user == null) {
			throw new IllegalArgumentException("User not found.");
		}

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("Current password is invalid.");
		}

		if (request.getEmail() != null && !request.getEmail().isBlank()) {
			String email = request.getEmail().trim();
			if (!EMAIL_PATTERN.matcher(email).matches()) {
				throw new IllegalArgumentException("A valid email address is required.");
			}
			// friendly 400 instead of a 500 from the DB unique constraint
			if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
				throw new IllegalArgumentException("An account with this email already exists.");
			}
			user.setEmail(email);
		}
		if (request.getUsername() != null && !request.getUsername().isBlank()) {
			String username = request.getUsername().trim();
			validateUsername(username);
			if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
				throw new IllegalArgumentException("That username is already taken.");
			}
			user.setUsername(username);
		}

		userRepository.save(user);

		UserDto dto = new UserDto();
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setUserRole(user.getUserRole());
		return dto;
	}
}
