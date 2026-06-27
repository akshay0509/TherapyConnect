package com.org.therapistService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_AVAILABILITY_OVERRIDES")
public class TherapistAvailabilityOverrides {

	@Id
	private String overrideId;
	
	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;
	
	private String reason;

    @Column(nullable = false)
    private Boolean syncToGoogleCalendar = false;
	
	private boolean isAvailable;
	
	@PrePersist
    public void generateId() {
        if (this.overrideId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.overrideId = "OVR" + uniquePart;
        }
    }
}
