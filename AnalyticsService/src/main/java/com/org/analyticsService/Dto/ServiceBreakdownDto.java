package com.org.analyticsService.Dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceBreakdownDto {
    private String serviceId;
    private int completedCount;
    private BigDecimal earnings;
}
