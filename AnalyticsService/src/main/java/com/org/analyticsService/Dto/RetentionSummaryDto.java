package com.org.analyticsService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetentionSummaryDto {
    private int totalUniqueClients;
    private int retainedClients;
    private int churnedClients;
    private double retentionRate;
    private double avgSessionsPerClient;
    private double avgClientLifetimeDays;
}
