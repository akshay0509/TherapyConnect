package com.org.therapistService.Dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDto {

    private long sessionsToday;
    private long activeClients;
    private long completedThisWeek;
    private BigDecimal dayEarnings;
	private BigDecimal weekEarnings;
	private BigDecimal monthEarnings;
	private BigDecimal lifetimeEarnings;

	/** Completed sessions across the practice that still have no notes. */
	private long pendingNotes;
}
