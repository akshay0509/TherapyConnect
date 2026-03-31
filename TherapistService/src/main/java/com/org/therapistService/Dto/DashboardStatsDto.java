package com.org.therapistService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDto {

    private long sessionsToday;
    private long activeClients;
    private long completedThisWeek;
}
