package com.org.appointmentService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENT_CONTACT_PROJECTION")
public class ClientContactProjection {

	@Id
	private String clientId;

	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;
}
