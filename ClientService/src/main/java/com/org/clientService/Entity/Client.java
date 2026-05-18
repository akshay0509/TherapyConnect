package com.org.clientService.Entity;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.UUID;

import com.org.events.Client.ClientStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENT")
public class Client {

	@Id
	private String clientId;

	private String therapistId;
	private String firstName;
	private String lastName;
	private Date dob;
	private int age;
	private String phoneNumber;
	private String emergencyPhoneNumber;
	private String email;
	private String pronouns;
	private String gender;
	private LocalDateTime createdAt = LocalDateTime.now();
	private ClientStatus status = ClientStatus.ACTIVE;
	private boolean dsf = false;
	
	@PrePersist
	public void generateId() {
		if (this.clientId == null) {
			String uniquePart = UUID.randomUUID().toString().substring(0, 8);
			this.clientId = "CLT" + uniquePart;
		}
	}
}
