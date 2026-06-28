package com.org.analyticsService.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailySnapshotDto {
    private LocalDate date;
    private int completedCount;
    private int cancelledCount;
    private int abandonedCount;
    private int rescheduledCount;
    private int paidCount;
    private int dsfCount;
    private int onlineCount;
    private int offlineCount;
    private BigDecimal earnings;
}
