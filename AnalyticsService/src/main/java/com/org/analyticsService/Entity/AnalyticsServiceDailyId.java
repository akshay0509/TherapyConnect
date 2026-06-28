package com.org.analyticsService.Entity;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsServiceDailyId implements Serializable {
    private String therapistId;
    private LocalDate date;
    private String serviceId;
}
