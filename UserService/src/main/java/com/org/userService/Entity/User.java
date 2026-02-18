package com.org.userService.Entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.org.userService.Enum.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "USERS")
public class User {

	@Id
	private String userId;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private boolean isEnabled = true;

	@Column(nullable = false)
	private boolean isAccountLocked = false;

	@Column(nullable = false)
	private int failedAttempts = 0;

	private Instant lastLoginTime;

	private String lastLoginIp;
	
	private String lastLoginUserAgent;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();
	
	@Enumerated(EnumType.STRING)
	private UserRole userRole;

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = Instant.now();
	}
	
	@ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "USER_ROLES",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Roles> roles;
	
	@PrePersist
    public void generateId() {
        if (this.userId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.userId = "USR" + uniquePart;
        }
    }
}
