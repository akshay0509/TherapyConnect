package com.org.userService.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.userService.Dto.AdminUserDto;
import com.org.userService.Entity.UserLoginAudit;
import com.org.userService.Services.AdminUserService;

@RestController
@RequestMapping("/admin")
public class AdminUserController {

	@Autowired
	AdminUserService adminUserService;

	@GetMapping("/users")
	public ResponseEntity<List<AdminUserDto>> getAllUsers() {
		return ResponseEntity.ok(adminUserService.getAllUsers());
	}

	@PutMapping("/users/{userId}/status")
	public ResponseEntity<?> updateUserStatus(@PathVariable String userId,
			@RequestBody Map<String, Boolean> body) {
		try {
			AdminUserDto updated = adminUserService.updateUserStatus(userId, body.get("enabled"), body.get("locked"));
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/audit")
	public ResponseEntity<List<UserLoginAudit>> getLoginAudit() {
		return ResponseEntity.ok(adminUserService.getRecentLoginAudit());
	}
}
