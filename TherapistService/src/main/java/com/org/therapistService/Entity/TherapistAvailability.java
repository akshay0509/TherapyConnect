package com.org.therapistService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.org.therapistService.Enums.SessionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_AVAILABILITY")
public class TherapistAvailability {

	@Id
	private String slotId;
	
	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;

	@Enumerated(EnumType.STRING)
	//@Column(name = "session_type", nullable = false)
	private SessionType sessionType;
	
	@Column(nullable = true)
	private String serviceId;
	
	@PrePersist
    public void generateId() {
        if (this.slotId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.slotId = "SLT" + uniquePart;
        }
    }
}
