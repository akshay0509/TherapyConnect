package com.org.therapistService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.org.events.Client.ClientStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_CLIENTS", uniqueConstraints =
@UniqueConstraint(columnNames =
{"therapistId", "clientId"}))
public class TherapistClients {

	@Id
	private String id;
	
	@Column(nullable = false)
	private String therapistId;
	
	@Column(nullable = false)
	private String clientId;
	
	private String clientName;
	private LocalDateTime createdAt = LocalDateTime.now();
	private ClientStatus status;
	
	@PrePersist
    public void generateId() {
        if (this.id == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.id = "ID" + uniquePart;
        }
    }
}
