package com.org.analyticsService.Dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyticsSummaryDto {
    private int totalCompleted;
    private int totalCancelled;
    private int totalAbandoned;
    private int totalRescheduled;
    private int totalPaid;
    private int totalDsf;
    private int totalOnline;
    private int totalOffline;
    private BigDecimal totalEarnings;
    private double completionRate;
    private double cancellationRate;
}
