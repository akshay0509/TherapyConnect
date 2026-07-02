package com.org.notificationService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_PROJECTION")
public class TherapistProjection {
    @Id
    private String therapistId;
    private String timezone;
}
