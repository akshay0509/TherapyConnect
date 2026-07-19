package com.org.appointmentService.Services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.appointmentService.Dto.AdminHealthDto;
import com.org.appointmentService.Entity.OutboxEvent;
import com.org.appointmentService.Repository.OutboxEventRepository;

@Service
public class AdminService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    // Events unpublished for more than 3 minutes indicate a problem
    private static final int STALE_THRESHOLD_MINUTES = 3;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    public AdminHealthDto getHealth() {
        AdminHealthDto dto = new AdminHealthDto();
        dto.setServerTime(LocalDateTime.now().format(ISO_FMT));

        // parked (poison) events are excluded from pending/staleness — they no
        // longer retry, so counting them would keep the card red forever;
        // they get their own counter instead
        long pendingCount = outboxEventRepository.countByPublishedFalseAndParkedFalse();
        Optional<OutboxEvent> oldest = outboxEventRepository.findTopByPublishedFalseAndParkedFalseOrderByCreatedAtAsc();

        AdminHealthDto.OutboxStatus outbox = new AdminHealthDto.OutboxStatus();
        outbox.setPendingCount(pendingCount);
        outbox.setParkedCount(outboxEventRepository.countByParkedTrue());

        if (pendingCount == 0 || oldest.isEmpty()) {
            outbox.setStatus("OK");
        } else {
            LocalDateTime oldestTime = oldest.get().getCreatedAt();
            outbox.setOldestPendingAt(oldestTime.format(ISO_FMT));

            boolean isStale = oldestTime.isBefore(LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES));
            if (isStale) {
                outbox.setStatus("STALE");
                outbox.setEstimatedIssueStartedAt(oldestTime.format(ISO_FMT));
            } else {
                outbox.setStatus("OK");
            }
        }

        dto.setOutbox(outbox);
        return dto;
    }

    public Map<String, Object> replayOutbox(LocalDateTime from) {
        int resetCount = outboxEventRepository.resetPublishedFrom(from);
        return Map.of(
            "resetCount", resetCount,
            "replayFrom", from.format(ISO_FMT),
            "message", "Reset " + resetCount + " events (parked events in range un-parked). Outbox scheduler will republish within seconds."
        );
    }
}
