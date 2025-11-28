package com.org.therapistService.Entity;

import java.time.LocalTime;
import java.util.UUID;

import com.org.therapistService.Enums.SessionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_AVAILABILITY_RULES", uniqueConstraints = {
	    @UniqueConstraint(columnNames = {"therapistId", "dayOfWeek", "startTime"}) 
	})
public class TherapistAvailabilityRules {

	@Id
	private String ruleId;
	
	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private LocalTime dayOfWeek;
	
	@Column(nullable = false)
	private LocalTime startTime;

	@Column(nullable = false)
	private LocalTime endTime;
	
	@Enumerated(EnumType.STRING)
	private SessionType sessionType;

	private boolean isActive;
	
	@PrePersist
    public void generateId() {
        if (this.ruleId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.ruleId = "RUL" + uniquePart;
        }
    }
}
