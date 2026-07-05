package com.org.analyticsService.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ledger of already-applied appointment events. The outbox publisher and Kafka
 * are both at-least-once, so counters/earnings would silently double on any
 * redelivery without this guard. Inserted in the same transaction as the
 * aggregate update.
 */
@Entity
@Table(name = "PROCESSED_EVENTS")
@Data
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }
}
