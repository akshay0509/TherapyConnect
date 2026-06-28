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
@IdClass(AnalyticsDailyId.class)
@Table(name = "ANALYTICS_DAILY")
public class AnalyticsDaily {

    @Id
    private String therapistId;

    @Id
    private LocalDate date;

    @Column(nullable = false)
    private int completedCount = 0;

    @Column(nullable = false)
    private int cancelledCount = 0;

    @Column(nullable = false)
    private int abandonedCount = 0;

    @Column(nullable = false)
    private int rescheduledCount = 0;

    @Column(nullable = false)
    private int paidCount = 0;

    @Column(nullable = false)
    private int dsfCount = 0;

    @Column(nullable = false)
    private int onlineCount = 0;

    @Column(nullable = false)
    private int offlineCount = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal earnings = BigDecimal.ZERO;

    public AnalyticsDaily(String therapistId, LocalDate date) {
        this.therapistId = therapistId;
        this.date = date;
    }
}
