package com.org.analyticsService.Entity;

import java.math.BigDecimal;
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
@IdClass(AnalyticsServiceDailyId.class)
@Table(name = "ANALYTICS_SERVICE_DAILY")
public class AnalyticsServiceDaily {

    @Id
    private String therapistId;

    @Id
    private LocalDate date;

    @Id
    private String serviceId;

    @Column(nullable = false)
    private int completedCount = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal earnings = BigDecimal.ZERO;

    public AnalyticsServiceDaily(String therapistId, LocalDate date, String serviceId) {
        this.therapistId = therapistId;
        this.date = date;
        this.serviceId = serviceId;
    }
}
