package com.org.analyticsService.Dto;

import lombok.Data;

@Data
public class AnalyticsAdminHealthDto {

    private String lastProcessedDate;  // ISO date, e.g. "2026-07-02"
    private long daysBehind;
    private String status; // "OK" | "BEHIND" | "NO_DATA"
    private String serverTime;
}
