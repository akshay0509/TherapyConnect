package com.org.notificationService.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENT_PROJECTION")
public class ClientProjection {

	@Id
	private String clientId;
	
	private String firstName;
	private String lastName;
	private String email;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
