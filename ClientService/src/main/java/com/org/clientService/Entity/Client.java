package com.org.clientService.Entity;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.UUID;

import com.org.events.Client.ClientStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	private String fullName;
	private Date dob;
	private int age;
	private String phoneNumber;
	private String emergencyPhoneNumber;
	private String email;
	private String pronouns;
	private String gender;
	private String qualification;
	private String currentOccupation;
	private String preferredDays;
	private String preferredTimings;
	private String preferredModes;
	private String emergencyContactName;
	private Integer emergencyContactAge;
	private String emergencyContactRelationship;
		/**
	 * Negotiated flat rate for this client. Null = no special rate, so the
	 * delivery-mode price applies. Overridden only by a custom fee entered at
	 * booking time. DSF clients earn nothing regardless of this value.
	 */
	private java.math.BigDecimal sessionFee;

	private String source = "MANUAL";
	private LocalDateTime createdAt = LocalDateTime.now();
	/* STRING, not the JPA default ORDINAL. An ordinal column stores position,
	   so adding or reordering a constant silently re-labels every existing row
	   — the UI already offers an "Archived" filter, and slotting ARCHIVED in
	   alphabetically would turn every TERMINATED client into it. */
	@Enumerated(EnumType.STRING)
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
