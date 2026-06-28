package com.org.therapistService.Dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EarningsSummaryDto {

    // Actual money earned (DSF sessions count as zero)
    private BigDecimal weekEarnings;
    private BigDecimal monthEarnings;
    private BigDecimal lifetimeEarnings;

    // Paid session counts (non-DSF completed sessions)
    private long weekPaidCount;
    private long monthPaidCount;
    private long lifetimePaidCount;

    // DSF (pro bono) completed session counts
    private long weekDsfCount;
    private long monthDsfCount;
    private long lifetimeDsfCount;
}
