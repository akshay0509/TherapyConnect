package com.org.therapistService.Dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EarningsSummaryDto {

    // Actual money earned (DSF sessions count as zero).
    // Billable = COMPLETED + ABANDONED — a no-show bills the full fee.
    private BigDecimal weekEarnings;
    private BigDecimal monthEarnings;
    private BigDecimal lifetimeEarnings;

    // Billable session counts (non-DSF, completed or no-show)
    private long weekPaidCount;
    private long monthPaidCount;
    private long lifetimePaidCount;

    // DSF (pro bono) completed session counts — delivered work, zero money
    private long weekDsfCount;
    private long monthDsfCount;
    private long lifetimeDsfCount;

    // Of the billable counts above, how many were no-shows. Broken out so the
    // therapist can see what share of income came from sessions nobody attended.
    private long weekAbandonedCount;
    private long monthAbandonedCount;
    private long lifetimeAbandonedCount;
}
