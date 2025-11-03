package com.org.therapistService.Entity;

import java.util.UUID;

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
@Table(name = "THERAPIST_SERVICES")
public class TherapistServices {

	@Id
	private String serviceId;
	
	@Column(nullable = false)
	private String therapistId;
	
	@Enumerated(EnumType.STRING)
	//@Column(name = "service_type", nullable = false)
	private ServiceType serviceType;
	
	private int duration;
	private float price;
	private boolean isActive;
	
	@PrePersist
    public void generateId() {
        if (this.serviceId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.serviceId = "SRV" + uniquePart;
        }
    }
	
	public enum ServiceType{
		INDIVIDUAL_THERAPY,
		COUPLES_THERAPY
	}
}
