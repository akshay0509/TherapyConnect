package com.org.userService.Entity;

import java.time.Instant;
import java.util.UUID;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "USER_LOGIN_AUDIT",
indexes = {
		@Index(name = "idx_login_audit_user", columnList = "userId"),
		@Index(name = "idx_login_audit_username", columnList = "username"),
		@Index(name = "idx_login_audit_time", columnList = "loginAt")
})
public class UserLoginAudit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private UUID userId;

	private String username;

	@Column(nullable = false)
	private Instant loginAt = Instant.now();

	private String ipAddress;
	
	private String userAgent;

	@Column(nullable = false)
	private boolean success;

	private String failureReason;

	public static UserLoginAudit success(UUID userId, String username, String ip, String userAgent) {
		UserLoginAudit audit = new UserLoginAudit();
		audit.userId = userId;
		audit.username = username;
		audit.ipAddress = ip;
		audit.userAgent = userAgent;
		audit.success = true;
		return audit;
	}

	public static UserLoginAudit failure(String username, String ip, String userAgent, String reason) {
		UserLoginAudit audit = new UserLoginAudit();
		audit.username = username;
		audit.ipAddress = ip;
		audit.userAgent = userAgent;
		audit.success = false;
		audit.failureReason = reason;
		return audit;
	}
}
