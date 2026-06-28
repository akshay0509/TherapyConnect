package com.org.analyticsService.Entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@IdClass(ClientEngagementProjectionId.class)
@Table(name = "ANALYTICS_CLIENT_ENGAGEMENT")
public class ClientEngagementProjection {

    @Id
    private String clientId;

    @Id
    private String therapistId;

    private LocalDate firstSessionDate;
    private LocalDate lastSessionDate;

    @Column(nullable = false)
    private int totalSessions = 0;

    public ClientEngagementProjection(String clientId, String therapistId, LocalDate firstSessionDate) {
        this.clientId = clientId;
        this.therapistId = therapistId;
        this.firstSessionDate = firstSessionDate;
        this.lastSessionDate = firstSessionDate;
        this.totalSessions = 0;
    }
}
