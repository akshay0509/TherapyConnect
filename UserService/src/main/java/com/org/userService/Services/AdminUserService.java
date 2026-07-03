package com.org.userService.Services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.org.userService.Dto.AdminUserDto;
import com.org.userService.Entity.User;
import com.org.userService.Entity.UserLoginAudit;
import com.org.userService.Repository.UserLoginAuditRepository;
import com.org.userService.Repository.UserRepository;

@Service
public class AdminUserService {

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserLoginAuditRepository userLoginAuditRepository;

	private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

	public List<AdminUserDto> getAllUsers() {
		return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
				.stream()
				.map(this::toDto)
				.toList();
	}

	public AdminUserDto updateUserStatus(String userId, Boolean enabled, Boolean locked) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		if (enabled != null) {
			user.setEnabled(enabled);
		}
		if (locked != null) {
			user.setAccountLocked(locked);
			// Unlocking clears the failed-attempt counter so the user gets a fresh start
			if (!locked) {
				user.setFailedAttempts(0);
			}
		}

		userRepository.save(user);
		logger.info("Admin updated user {}: enabled={}, locked={}", userId, user.isEnabled(), user.isAccountLocked());
		return toDto(user);
	}

	public List<UserLoginAudit> getRecentLoginAudit() {
		return userLoginAuditRepository.findTop100ByOrderByLoginAtDesc();
	}

	private AdminUserDto toDto(User user) {
		AdminUserDto dto = new AdminUserDto();
		dto.setUserId(user.getUserId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setUserRole(user.getUserRole());
		dto.setEnabled(user.isEnabled());
		dto.setAccountLocked(user.isAccountLocked());
		dto.setFailedAttempts(user.getFailedAttempts());
		dto.setLastLoginTime(user.getLastLoginTime());
		dto.setCreatedAt(user.getCreatedAt());
		return dto;
	}
}
