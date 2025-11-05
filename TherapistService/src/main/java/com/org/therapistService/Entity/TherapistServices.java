package com.org.therapistService.Entity;

import java.util.UUID;

import com.org.therapistService.Enums.ServiceType;

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
@Table(name = "THERAPIST_SERVICES", uniqueConstraints = {
	    @UniqueConstraint(columnNames = {"therapistId", "serviceType"}) 
	})
public class TherapistServices {

	@Id
	private String serviceId;
	
	@Column(nullable = false)
	private String therapistId;
	
	@Enumerated(EnumType.STRING)
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
}
