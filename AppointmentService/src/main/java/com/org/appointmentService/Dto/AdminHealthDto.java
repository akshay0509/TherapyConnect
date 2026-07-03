package com.org.appointmentService.Dto;

import lombok.Data;

@Data
public class AdminHealthDto {

    private OutboxStatus outbox;
    private String serverTime;

    @Data
    public static class OutboxStatus {
        private long pendingCount;
        private String oldestPendingAt;
        private String estimatedIssueStartedAt;
        private String status; // "OK" | "STALE"
    }
}
